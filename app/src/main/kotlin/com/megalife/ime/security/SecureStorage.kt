package com.megalife.ime.security

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted SharedPreferences wrapper for sensitive data.
 * Backed by Android Keystore master key.
 */
class SecureStorage(context: Context) {

    companion object {
        private const val FILE_NAME = "megalife_secure_prefs"
        private const val KEY_AZURE_KEY = "azure_speech_key"
        private const val KEY_AZURE_REGION = "azure_speech_region"
        private const val DEFAULT_REGION = "westeurope"

        @Volatile
        private var instance: SecureStorage? = null

        fun getInstance(context: Context): SecureStorage {
            return instance ?: synchronized(this) {
                instance ?: SecureStorage(context.applicationContext).also { instance = it }
            }
        }
    }

    private val encryptedPrefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        migrateFromPlainPrefs(context)
    }

    fun getAzureKey(): String {
        return encryptedPrefs.getString(KEY_AZURE_KEY, "") ?: ""
    }

    fun setAzureKey(key: String) {
        encryptedPrefs.edit().putString(KEY_AZURE_KEY, key).apply()
    }

    fun getAzureRegion(): String {
        return encryptedPrefs.getString(KEY_AZURE_REGION, DEFAULT_REGION) ?: DEFAULT_REGION
    }

    fun setAzureRegion(region: String) {
        encryptedPrefs.edit().putString(KEY_AZURE_REGION, region).apply()
    }

    /**
     * One-time migration: move plaintext Azure credentials from default prefs
     * into encrypted storage, then delete the plaintext entries.
     */
    private fun migrateFromPlainPrefs(context: Context) {
        val plainPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val migratedKey = "secure_storage_migrated"

        if (plainPrefs.getBoolean(migratedKey, false)) return

        val oldKey = plainPrefs.getString("azure_speech_key", null)
        val oldRegion = plainPrefs.getString("azure_speech_region", null)

        // Write to encrypted prefs synchronously first
        if (!oldKey.isNullOrEmpty()) {
            encryptedPrefs.edit().putString(KEY_AZURE_KEY, oldKey).commit()
        }
        if (!oldRegion.isNullOrEmpty()) {
            encryptedPrefs.edit().putString(KEY_AZURE_REGION, oldRegion).commit()
        }

        // Only delete plaintext keys after encrypted writes are confirmed
        plainPrefs.edit()
            .remove("azure_speech_key")
            .remove("azure_speech_region")
            .putBoolean(migratedKey, true)
            .commit()
    }
}
