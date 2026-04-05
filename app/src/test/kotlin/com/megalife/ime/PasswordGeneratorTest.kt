package com.megalife.ime

import com.megalife.ime.autofill.PasswordGenerator
import org.junit.Assert.*
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun `generated password has correct length`() {
        val password = PasswordGenerator.generate(length = 16)
        assertEquals(16, password.length)
    }

    @Test
    fun `default strong password is 20 characters`() {
        val password = PasswordGenerator.generateStrong()
        assertEquals(20, password.length)
    }

    @Test
    fun `password with all classes contains uppercase`() {
        // Generate several passwords to reduce flakiness
        val passwords = (1..10).map {
            PasswordGenerator.generate(length = 20, uppercase = true, lowercase = true, digits = true, symbols = true)
        }
        assertTrue("At least one password should contain uppercase",
            passwords.all { it.any { c -> c.isUpperCase() } })
    }

    @Test
    fun `password with all classes contains lowercase`() {
        val passwords = (1..10).map {
            PasswordGenerator.generate(length = 20, uppercase = true, lowercase = true, digits = true, symbols = true)
        }
        assertTrue("All passwords should contain lowercase",
            passwords.all { it.any { c -> c.isLowerCase() } })
    }

    @Test
    fun `password with all classes contains digits`() {
        val passwords = (1..10).map {
            PasswordGenerator.generate(length = 20, uppercase = true, lowercase = true, digits = true, symbols = true)
        }
        assertTrue("All passwords should contain digits",
            passwords.all { it.any { c -> c.isDigit() } })
    }

    @Test
    fun `password with all classes contains symbols`() {
        val symbols = "!@#\$%^&*()-_=+[]{}|;:',.<>?/~`"
        val passwords = (1..10).map {
            PasswordGenerator.generate(length = 20, uppercase = true, lowercase = true, digits = true, symbols = true)
        }
        assertTrue("All passwords should contain symbols",
            passwords.all { pw -> pw.any { c -> c in symbols } })
    }

    @Test
    fun `pin mode generates only digits`() {
        val pin = PasswordGenerator.generatePin(6)
        assertEquals(6, pin.length)
        assertTrue("PIN should contain only digits", pin.all { it.isDigit() })
    }

    @Test
    fun `pin mode with custom length`() {
        val pin = PasswordGenerator.generatePin(4)
        assertEquals(4, pin.length)
        assertTrue("PIN should contain only digits", pin.all { it.isDigit() })
    }

    @Test
    fun `only digits when other classes disabled`() {
        val password = PasswordGenerator.generate(
            length = 10,
            uppercase = false,
            lowercase = false,
            digits = true,
            symbols = false
        )
        assertEquals(10, password.length)
        assertTrue("Should contain only digits", password.all { it.isDigit() })
    }

    @Test
    fun `only uppercase when other classes disabled`() {
        val password = PasswordGenerator.generate(
            length = 12,
            uppercase = true,
            lowercase = false,
            digits = false,
            symbols = false
        )
        assertEquals(12, password.length)
        assertTrue("Should contain only uppercase", password.all { it.isUpperCase() })
    }

    @Test
    fun `minimum length is enforced to number of enabled classes`() {
        // Request length 1 but 3 classes enabled -> should get at least 3
        val password = PasswordGenerator.generate(
            length = 1,
            uppercase = true,
            lowercase = true,
            digits = true,
            symbols = false
        )
        assertTrue("Length should be at least 3 (number of enabled classes)", password.length >= 3)
    }

    @Test
    fun `zero length with one class returns at least 1`() {
        val password = PasswordGenerator.generate(
            length = 0,
            uppercase = false,
            lowercase = false,
            digits = true,
            symbols = false
        )
        assertTrue("Should have at least 1 character", password.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no character classes throws exception`() {
        PasswordGenerator.generate(
            length = 10,
            uppercase = false,
            lowercase = false,
            digits = false,
            symbols = false
        )
    }

    @Test
    fun `generated passwords are not identical`() {
        val passwords = (1..5).map { PasswordGenerator.generateStrong() }.toSet()
        assertTrue("Should generate different passwords", passwords.size > 1)
    }
}
