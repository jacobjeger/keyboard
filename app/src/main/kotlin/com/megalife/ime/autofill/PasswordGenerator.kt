package com.megalife.ime.autofill

import java.security.SecureRandom

/**
 * Cryptographically secure password generator.
 * Uses SecureRandom and guarantees at least one character from each enabled class.
 */
object PasswordGenerator {

    private val random = SecureRandom()

    private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGIT_CHARS = "0123456789"
    private const val SYMBOL_CHARS = "!@#\$%^&*()-_=+[]{}|;:',.<>?/~`"

    /**
     * Generate a password with the given constraints.
     *
     * @param length Total length of the password (minimum 4, or number of enabled classes)
     * @param uppercase Include uppercase letters
     * @param lowercase Include lowercase letters
     * @param digits Include digits
     * @param symbols Include symbols
     * @return Generated password string
     * @throws IllegalArgumentException if no character classes are enabled or length is too small
     */
    fun generate(
        length: Int = 20,
        uppercase: Boolean = true,
        lowercase: Boolean = true,
        digits: Boolean = true,
        symbols: Boolean = true
    ): String {
        val pools = mutableListOf<String>()
        if (uppercase) pools.add(UPPERCASE_CHARS)
        if (lowercase) pools.add(LOWERCASE_CHARS)
        if (digits) pools.add(DIGIT_CHARS)
        if (symbols) pools.add(SYMBOL_CHARS)

        require(pools.isNotEmpty()) { "At least one character class must be enabled" }

        val effectiveLength = length.coerceAtLeast(pools.size)
        val allChars = pools.joinToString("")
        val result = CharArray(effectiveLength)

        // Guarantee at least one character from each enabled class
        val mandatoryPositions = (0 until effectiveLength).shuffled(random).take(pools.size)
        for ((i, pool) in pools.withIndex()) {
            result[mandatoryPositions[i]] = pool[random.nextInt(pool.length)]
        }

        // Fill remaining positions from the combined pool
        val mandatorySet = mandatoryPositions.toSet()
        for (i in 0 until effectiveLength) {
            if (i !in mandatorySet) {
                result[i] = allChars[random.nextInt(allChars.length)]
            }
        }

        return String(result)
    }

    /** Generate a strong 20-character password with all character classes. */
    fun generateStrong(): String = generate(20, uppercase = true, lowercase = true, digits = true, symbols = true)

    /** Generate a numeric PIN of the given length. */
    fun generatePin(length: Int = 6): String = generate(length, uppercase = false, lowercase = false, digits = true, symbols = false)
}
