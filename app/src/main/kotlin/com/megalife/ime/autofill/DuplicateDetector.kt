package com.megalife.ime.autofill

/**
 * Detects duplicate credentials to avoid saving the same domain+username pair multiple times.
 */
class DuplicateDetector(private val repository: CredentialRepository) {

    /**
     * Check if a credential with the same domain and username already exists.
     */
    suspend fun isDuplicate(domain: String, username: String): Boolean {
        return repository.findByDomainAndUsername(domain, username) != null
    }

    /**
     * Find all existing credentials for a given domain.
     */
    suspend fun findExisting(domain: String): List<CredentialRepository.DecryptedCredential> {
        return repository.findByDomain(domain)
    }
}
