package com.megalife.ime.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import com.megalife.ime.R
import com.megalife.ime.db.MegaLifeDatabase
import com.megalife.ime.settings.PreferenceKeys
import kotlinx.coroutines.*

/**
 * Android Autofill Service for MegaLife IME.
 * Detects login fields, offers saved credentials, and saves new ones.
 */
class MegaLifeAutofillService : AutofillService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val credentialRepository by lazy {
        CredentialRepository(MegaLifeDatabase.getInstance(this).credentialDao())
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        val fields = parseStructure(structure)
        if (fields.usernameId == null && fields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        // Check if autofill is enabled
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!prefs.getBoolean(PreferenceKeys.AUTOFILL_ENABLED, true)) {
            callback.onSuccess(null)
            return
        }

        val domain = fields.webDomain ?: fields.packageName ?: ""
        val requireBiometric = prefs.getBoolean(PreferenceKeys.AUTOFILL_BIOMETRIC, true)

        scope.launch {
            try {
                val credentials = credentialRepository.findByDomain(domain)
                if (credentials.isEmpty()) {
                    // No saved credentials — offer to save via SaveInfo
                    val response = buildSaveResponse(fields)
                    withContext(Dispatchers.Main) {
                        callback.onSuccess(response)
                    }
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()

                if (requireBiometric) {
                    // Require device authentication before filling credentials
                    responseBuilder.setFlags(FillResponse.FLAG_DELAY_FILL)
                }

                for (cred in credentials) {
                    val presentation = RemoteViews(packageName, R.layout.credential_prompt).apply {
                        setTextViewText(R.id.credential_label, "${cred.username} (${cred.displayName})")
                    }

                    @Suppress("DEPRECATION")
                    val datasetBuilder = Dataset.Builder(presentation)

                    fields.usernameId?.let { id ->
                        @Suppress("DEPRECATION")
                        datasetBuilder.setValue(id, AutofillValue.forText(cred.username))
                    }
                    fields.passwordId?.let { id ->
                        @Suppress("DEPRECATION")
                        datasetBuilder.setValue(id, AutofillValue.forText(cred.password))
                    }

                    if (requireBiometric) {
                        datasetBuilder.setAuthentication(null) // System handles device credential
                    }

                    responseBuilder.addDataset(datasetBuilder.build())
                }

                // Also offer save for new credentials
                addSaveInfo(responseBuilder, fields)

                withContext(Dispatchers.Main) {
                    callback.onSuccess(responseBuilder.build())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onSuccess(null)
                }
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onFailure("No data to save")
            return
        }

        val fields = parseStructure(structure)
        val username = fields.usernameValue ?: ""
        val password = fields.passwordValue ?: ""
        val domain = fields.webDomain ?: fields.packageName ?: ""

        if (username.isEmpty() && password.isEmpty()) {
            callback.onFailure("No credentials to save")
            return
        }

        scope.launch {
            try {
                credentialRepository.save(
                    domain = domain,
                    username = username,
                    password = password,
                    displayName = domain
                )
                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("MegaLifeAutofill", "Failed to save credential", e)
                withContext(Dispatchers.Main) {
                    callback.onFailure("Failed to save credential")
                }
            }
        }
    }

    private fun buildSaveResponse(fields: ParsedFields): FillResponse? {
        val builder = FillResponse.Builder()
        addSaveInfo(builder, fields)
        return try {
            builder.build()
        } catch (e: Exception) {
            null
        }
    }

    private fun addSaveInfo(builder: FillResponse.Builder, fields: ParsedFields) {
        val ids = mutableListOf<AutofillId>()
        fields.usernameId?.let { ids.add(it) }
        fields.passwordId?.let { ids.add(it) }

        if (ids.isNotEmpty()) {
            builder.setSaveInfo(
                SaveInfo.Builder(
                    SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                    ids.toTypedArray()
                ).build()
            )
        }
    }

    private fun parseStructure(structure: AssistStructure): ParsedFields {
        val fields = ParsedFields()
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            parseNode(windowNode.rootViewNode, fields)
        }
        return fields
    }

    private fun parseNode(node: AssistStructure.ViewNode, fields: ParsedFields) {
        val hints = node.autofillHints
        val inputType = node.inputType

        if (fields.webDomain == null) {
            node.webDomain?.let { fields.webDomain = it }
        }
        if (fields.packageName == null) {
            node.idPackage?.let { fields.packageName = it }
        }

        if (hints != null) {
            for (hint in hints) {
                when {
                    hint.contains("username", ignoreCase = true) ||
                    hint.contains("email", ignoreCase = true) -> {
                        fields.usernameId = node.autofillId
                        fields.usernameValue = node.text?.toString()
                    }
                    hint.contains("password", ignoreCase = true) -> {
                        fields.passwordId = node.autofillId
                        fields.passwordValue = node.text?.toString()
                    }
                }
            }
        } else if (node.autofillId != null) {
            // Heuristic detection based on input type
            val isPassword = (inputType and android.text.InputType.TYPE_MASK_VARIATION) in listOf(
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )
            val isEmail = (inputType and android.text.InputType.TYPE_MASK_VARIATION) ==
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            if (isPassword && fields.passwordId == null) {
                fields.passwordId = node.autofillId
                fields.passwordValue = node.text?.toString()
            } else if (isEmail && fields.usernameId == null) {
                fields.usernameId = node.autofillId
                fields.usernameValue = node.text?.toString()
            }
        }

        for (i in 0 until node.childCount) {
            parseNode(node.getChildAt(i), fields)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private data class ParsedFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var usernameValue: String? = null,
        var passwordValue: String? = null,
        var webDomain: String? = null,
        var packageName: String? = null
    )
}
