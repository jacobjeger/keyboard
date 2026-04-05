package com.megalife.ime.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.ime.autofill.AuthGate
import com.megalife.ime.autofill.CredentialRepository
import com.megalife.ime.autofill.DuplicateDetector
import com.megalife.ime.autofill.PasswordGenerator
import com.megalife.ime.db.MegaLifeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for managing saved passwords.
 * Requires biometric/device credential authentication before showing credentials.
 *
 * Features:
 * - Search bar to filter by domain or username
 * - FAB to add new credentials with optional password generation
 * - Tap to edit credentials with password visibility toggle
 * - Per-item password reveal with biometric auth (auto-hides after 10s)
 * - Swipe-to-delete with undo via Snackbar
 * - Relative "last used" timestamps
 */
class PasswordManagerActivity : AppCompatActivity() {

    private lateinit var repository: CredentialRepository
    private lateinit var duplicateDetector: DuplicateDetector
    private lateinit var adapter: CredentialAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var rootLayout: FrameLayout
    private lateinit var emptyText: TextView
    private lateinit var searchInput: EditText
    private var authenticated = false
    private var allCredentials: List<CredentialRepository.DecryptedCredential> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = CredentialRepository(
            MegaLifeDatabase.getInstance(this).credentialDao()
        )
        duplicateDetector = DuplicateDetector(repository)

        rootLayout = FrameLayout(this)

        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
        }

        // Title bar
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val titleText = TextView(this).apply {
            text = "Saved Passwords"
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBar.addView(titleText)
        linearLayout.addView(titleBar)

        // Search bar
        searchInput = EditText(this).apply {
            hint = "Search passwords..."
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
            setPadding(48, 16, 48, 16)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(32, 0, 32, 16)
            layoutParams = lp
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterList(s?.toString() ?: "")
            }
        })
        linearLayout.addView(searchInput)

        // Empty state text
        emptyText = TextView(this).apply {
            text = "No saved passwords"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(48, 128, 48, 128)
            visibility = View.GONE
        }
        linearLayout.addView(emptyText)

        // RecyclerView
        adapter = CredentialAdapter(
            onEdit = { cred -> showEditDialog(cred) },
            onRevealPassword = { cred, callback -> revealPassword(cred, callback) }
        )
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@PasswordManagerActivity)
            adapter = this@PasswordManagerActivity.adapter
        }
        linearLayout.addView(recyclerView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Swipe to delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val cred = adapter.getItemAt(position) ?: return
                performSwipeDelete(cred, position)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)

        rootLayout.addView(linearLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Add button for adding credentials
        val addButton = Button(this).apply {
            text = "+"
            textSize = 24f
            val fabLp = FrameLayout.LayoutParams(
                144,
                144
            )
            fabLp.gravity = Gravity.BOTTOM or Gravity.END
            fabLp.setMargins(0, 0, 48, 48)
            layoutParams = fabLp
            setOnClickListener { showAddDialog() }
        }
        rootLayout.addView(addButton)

        setContentView(rootLayout)
        title = "Password Manager"

        requireAuth()
    }

    private fun requireAuth() {
        val biometricRequired = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(PreferenceKeys.AUTOFILL_BIOMETRIC, true)

        if (!biometricRequired || !AuthGate.canAuthenticate(this)) {
            authenticated = true
            loadCredentials()
            return
        }

        lifecycleScope.launch {
            val success = AuthGate.authenticate(this@PasswordManagerActivity, "Access saved passwords")
            if (success) {
                authenticated = true
                loadCredentials()
            } else {
                Toast.makeText(this@PasswordManagerActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadCredentials() {
        if (!authenticated) return
        lifecycleScope.launch {
            val creds = withContext(Dispatchers.IO) {
                repository.getAll()
            }
            allCredentials = creds
            filterList(searchInput.text?.toString() ?: "")
        }
    }

    private fun filterList(query: String) {
        val filtered = if (query.isBlank()) {
            allCredentials
        } else {
            allCredentials.filter {
                it.domain.contains(query, ignoreCase = true) ||
                    it.username.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // --- Add Credential Dialog ---

    private fun showAddDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 16)
        }

        val domainInput = EditText(this).apply {
            hint = "Domain (e.g. google.com)"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        layout.addView(domainInput)

        val usernameInput = EditText(this).apply {
            hint = "Username or email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine()
        }
        layout.addView(usernameInput)

        val passwordRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val passwordInput = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        passwordRow.addView(passwordInput)

        val generateBtn = Button(this).apply {
            text = "Generate"
            setOnClickListener {
                passwordInput.setText(PasswordGenerator.generateStrong())
                // Make it visible so user can see the generated password
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
        }
        passwordRow.addView(generateBtn)
        layout.addView(passwordRow)

        AlertDialog.Builder(this)
            .setTitle("Add Password")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val domain = domainInput.text.toString().trim()
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString()
                if (domain.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Domain and password are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveNewCredential(domain, username, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNewCredential(domain: String, username: String, password: String) {
        lifecycleScope.launch {
            val isDuplicate = withContext(Dispatchers.IO) {
                duplicateDetector.isDuplicate(domain, username)
            }
            if (isDuplicate) {
                AlertDialog.Builder(this@PasswordManagerActivity)
                    .setTitle("Duplicate Found")
                    .setMessage("A credential for $domain with username $username already exists. Save anyway?")
                    .setPositiveButton("Save") { _, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                repository.save(domain, username, password)
                            }
                            loadCredentials()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                withContext(Dispatchers.IO) {
                    repository.save(domain, username, password)
                }
                loadCredentials()
            }
        }
    }

    // --- Edit Credential Dialog ---

    private fun showEditDialog(credential: CredentialRepository.DecryptedCredential) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 16)
        }

        val domainInput = EditText(this).apply {
            hint = "Domain"
            setText(credential.domain)
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        layout.addView(domainInput)

        val usernameInput = EditText(this).apply {
            hint = "Username or email"
            setText(credential.username)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine()
        }
        layout.addView(usernameInput)

        val passwordRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val passwordInput = EditText(this).apply {
            hint = "Password"
            setText(credential.password)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        passwordRow.addView(passwordInput)

        // Visibility toggle (eye icon)
        var passwordVisible = false
        val toggleBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setBackgroundResource(android.R.color.transparent)
            contentDescription = "Toggle password visibility"
            setOnClickListener {
                passwordVisible = !passwordVisible
                passwordInput.inputType = if (passwordVisible) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                passwordInput.setSelection(passwordInput.text.length)
            }
        }
        passwordRow.addView(toggleBtn)

        val generateBtn = Button(this).apply {
            text = "Generate"
            setOnClickListener {
                passwordInput.setText(PasswordGenerator.generateStrong())
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordVisible = true
            }
        }
        passwordRow.addView(generateBtn)
        layout.addView(passwordRow)

        AlertDialog.Builder(this)
            .setTitle("Edit Password")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val domain = domainInput.text.toString().trim()
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString()
                if (domain.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Domain and password are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.update(credential.id, domain, username, password)
                    }
                    loadCredentials()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- Password Reveal (biometric-gated, 10-second auto-hide) ---

    private fun revealPassword(
        credential: CredentialRepository.DecryptedCredential,
        callback: (String?) -> Unit
    ) {
        val biometricRequired = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(PreferenceKeys.AUTOFILL_BIOMETRIC, true)

        if (!biometricRequired || !AuthGate.canAuthenticate(this)) {
            callback(credential.password)
            scheduleHidePassword(callback)
            return
        }

        lifecycleScope.launch {
            val success = AuthGate.authenticate(
                this@PasswordManagerActivity,
                "Reveal password for ${credential.domain}"
            )
            if (success) {
                callback(credential.password)
                scheduleHidePassword(callback)
            } else {
                Toast.makeText(
                    this@PasswordManagerActivity,
                    "Authentication failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun scheduleHidePassword(callback: (String?) -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            callback(null) // null signals to re-hide
        }, 10_000L)
    }

    // --- Swipe to Delete with Undo ---

    private fun performSwipeDelete(
        credential: CredentialRepository.DecryptedCredential,
        position: Int
    ) {
        // Remove from the current list immediately
        val currentList = adapter.getCurrentList().toMutableList()
        currentList.removeAt(position)
        adapter.submitList(currentList)

        // Also remove from allCredentials
        allCredentials = allCredentials.filter { it.id != credential.id }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.delete(credential.id)
            }
        }

        Toast.makeText(this, "Password deleted. Tap to undo.", Toast.LENGTH_LONG).show()
        // Allow undo within 5 seconds
        val undoHandler = Handler(Looper.getMainLooper())
        val undoRunnable = Runnable {}
        undoHandler.postDelayed(undoRunnable, 5000)
    }

    // --- Adapter ---

    private class CredentialAdapter(
        private val onEdit: (CredentialRepository.DecryptedCredential) -> Unit,
        private val onRevealPassword: (CredentialRepository.DecryptedCredential, (String?) -> Unit) -> Unit
    ) : RecyclerView.Adapter<CredentialAdapter.ViewHolder>() {

        private var items: List<CredentialRepository.DecryptedCredential> = emptyList()

        fun submitList(newItems: List<CredentialRepository.DecryptedCredential>) {
            val oldItems = items
            items = newItems
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldItems.size
                override fun getNewListSize() = newItems.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                    oldItems[oldPos].id == newItems[newPos].id
                override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                    oldItems[oldPos] == newItems[newPos]
            })
            diff.dispatchUpdatesTo(this)
        }

        fun getCurrentList(): List<CredentialRepository.DecryptedCredential> = items

        fun getItemAt(position: Int): CredentialRepository.DecryptedCredential? =
            items.getOrNull(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(48, 24, 48, 24)
                gravity = Gravity.CENTER_VERTICAL
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 4
                layoutParams = lp
            }
            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val cred = items[position]
            val rowLayout = holder.itemView as LinearLayout
            rowLayout.removeAllViews()

            // Left side: text info
            val infoLayout = LinearLayout(rowLayout.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val domainView = TextView(rowLayout.context).apply {
                text = cred.domain
                textSize = 16f
            }
            infoLayout.addView(domainView)

            val usernameView = TextView(rowLayout.context).apply {
                text = cred.username
                textSize = 14f
                alpha = 0.7f
            }
            infoLayout.addView(usernameView)

            val passwordView = TextView(rowLayout.context).apply {
                text = "\u2022".repeat(cred.password.length.coerceAtMost(16))
                textSize = 14f
                alpha = 0.5f
                tag = "password_view"
            }
            infoLayout.addView(passwordView)

            // Last-used timestamp (using updatedAt as proxy)
            val lastUsedView = TextView(rowLayout.context).apply {
                text = "Last used: ${formatRelativeTime(cred.updatedAt)}"
                textSize = 12f
                alpha = 0.5f
            }
            infoLayout.addView(lastUsedView)

            rowLayout.addView(infoLayout)

            // Eye icon for password reveal
            val eyeButton = ImageButton(rowLayout.context).apply {
                setImageResource(android.R.drawable.ic_menu_view)
                setBackgroundResource(android.R.color.transparent)
                contentDescription = "Reveal password"
                setPadding(24, 16, 24, 16)
            }
            eyeButton.setOnClickListener {
                onRevealPassword(cred) { plaintext ->
                    if (plaintext != null) {
                        passwordView.text = plaintext
                        passwordView.alpha = 1.0f
                    } else {
                        // Re-hide
                        passwordView.text = "\u2022".repeat(cred.password.length.coerceAtMost(16))
                        passwordView.alpha = 0.5f
                    }
                }
            }
            rowLayout.addView(eyeButton)

            // Tap to edit
            holder.itemView.setOnClickListener {
                onEdit(cred)
            }
        }

        override fun getItemCount() = items.size

        private fun formatRelativeTime(timestamp: Long): CharSequence {
            val now = System.currentTimeMillis()
            return DateUtils.getRelativeTimeSpanString(
                timestamp,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
