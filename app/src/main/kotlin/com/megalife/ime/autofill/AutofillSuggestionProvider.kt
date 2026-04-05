package com.megalife.ime.autofill

import android.view.inputmethod.EditorInfo

/**
 * Provides autofill credential suggestions for the IME suggestion bar.
 * Queries saved credentials by the current app's package name or web domain.
 */
class AutofillSuggestionProvider(private val repository: CredentialRepository) {

    /**
     * Find matching credentials for the current input field.
     * Returns credentials matching the app package or web domain.
     */
    suspend fun findCredentials(editorInfo: EditorInfo): List<CredentialRepository.DecryptedCredential> {
        val packageName = editorInfo.packageName ?: return emptyList()

        // Try package name first
        val byPackage = repository.findByDomain(packageName)
        if (byPackage.isNotEmpty()) return byPackage

        // Try field extras for web domain
        val extras = editorInfo.extras
        val webDomain = extras?.getString("com.android.browser.last_url")
            ?: extras?.getString("android.autofill.webDomain")

        if (!webDomain.isNullOrEmpty()) {
            val domain = extractDomain(webDomain)
            return repository.findByDomain(domain)
        }

        return emptyList()
    }

    private fun extractDomain(url: String): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore(":")
    }
}
