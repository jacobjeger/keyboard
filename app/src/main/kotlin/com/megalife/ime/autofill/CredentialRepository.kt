package com.megalife.ime.autofill

import android.util.Log
import com.megalife.ime.db.dao.CredentialDao
import com.megalife.ime.db.entity.SavedCredential
import com.megalife.ime.security.KeystoreEncryption

/**
 * Manages saved credentials with transparent encryption/decryption.
 * Username and password fields are encrypted at rest using Android Keystore.
 */
class CredentialRepository(private val dao: CredentialDao) {

    companion object {
        private const val TAG = "CredentialRepo"
    }

    suspend fun save(domain: String, username: String, password: String): Long {
        return save(domain, username, password, domain)
    }

    suspend fun save(domain: String, username: String, password: String, displayName: String): Long {
        val credential = SavedCredential(
            domain = domain,
            username = KeystoreEncryption.encrypt(username),
            password = KeystoreEncryption.encrypt(password),
            displayName = displayName
        )
        return dao.insert(credential)
    }

    /**
     * Update a credential by id with new plaintext values.
     */
    suspend fun update(id: Long, domain: String, username: String, password: String) {
        val existing = dao.findByDomain(domain).firstOrNull { it.id == id }
            ?: dao.getAll().firstOrNull { it.id == id }
            ?: return
        update(DecryptedCredential(
            id = id,
            domain = domain,
            username = username,
            password = password,
            displayName = existing.displayName,
            createdAt = existing.createdAt,
            updatedAt = System.currentTimeMillis()
        ))
    }

    /**
     * Update a credential. Accepts a DecryptedCredential (plaintext fields)
     * and re-encrypts before storing.
     */
    suspend fun update(decrypted: DecryptedCredential) {
        dao.update(
            SavedCredential(
                id = decrypted.id,
                domain = decrypted.domain,
                username = KeystoreEncryption.encrypt(decrypted.username),
                password = KeystoreEncryption.encrypt(decrypted.password),
                displayName = decrypted.displayName,
                createdAt = decrypted.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun findByDomain(domain: String): List<DecryptedCredential> {
        return dao.findByDomain(domain).mapNotNull { decrypt(it) }
    }

    /**
     * Find a credential by domain and username. Since username is encrypted,
     * we query by domain in SQL and filter by decrypted username in memory.
     */
    suspend fun findByDomainAndUsername(domain: String, username: String): DecryptedCredential? {
        return dao.findByDomain(domain)
            .mapNotNull { decrypt(it) }
            .firstOrNull { it.username.equals(username, ignoreCase = true) }
    }

    /**
     * Search credentials by query string. Domain is searched in SQL;
     * username is searched in-memory after decryption since it's encrypted.
     */
    suspend fun search(query: String): List<DecryptedCredential> {
        if (query.isBlank()) return getAll()
        // Get domain matches from SQL
        val domainMatches = dao.searchByDomain(query).mapNotNull { decrypt(it) }
        // Also check all credentials for username matches (encrypted, must decrypt first)
        val allDecrypted = dao.getAll().mapNotNull { decrypt(it) }
        val usernameMatches = allDecrypted.filter {
            it.username.contains(query, ignoreCase = true)
        }
        // Merge and deduplicate by id
        return (domainMatches + usernameMatches)
            .distinctBy { it.id }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun getAll(): List<DecryptedCredential> {
        return dao.getAll().mapNotNull { decrypt(it) }
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }

    suspend fun count(): Int = dao.count()

    private fun decrypt(credential: SavedCredential): DecryptedCredential? {
        val username = KeystoreEncryption.decrypt(credential.username)
        val password = KeystoreEncryption.decrypt(credential.password)

        if (username == null || password == null) {
            Log.w(TAG, "Failed to decrypt credential id=${credential.id} for domain=${credential.domain}")
            return null
        }

        return DecryptedCredential(
            id = credential.id,
            domain = credential.domain,
            username = username,
            password = password,
            displayName = credential.displayName,
            createdAt = credential.createdAt,
            updatedAt = credential.updatedAt
        )
    }

    data class DecryptedCredential(
        val id: Long,
        val domain: String,
        val username: String,
        val password: String,
        val displayName: String,
        val createdAt: Long,
        val updatedAt: Long
    )
}
