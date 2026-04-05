package com.megalife.ime

import com.megalife.ime.autofill.CredentialExporter
import org.junit.Assert.*
import org.junit.Test

class CredentialExporterTest {

    // --- escapeCsv tests ---

    @Test
    fun `escapeCsv returns value unchanged when no special characters`() {
        assertEquals("hello", CredentialExporter.escapeCsv("hello"))
    }

    @Test
    fun `escapeCsv wraps in quotes when value contains comma`() {
        assertEquals("\"hello,world\"", CredentialExporter.escapeCsv("hello,world"))
    }

    @Test
    fun `escapeCsv wraps in quotes and doubles quotes when value contains quote`() {
        assertEquals("\"say \"\"hello\"\"\"", CredentialExporter.escapeCsv("say \"hello\""))
    }

    @Test
    fun `escapeCsv wraps in quotes when value contains newline`() {
        assertEquals("\"line1\nline2\"", CredentialExporter.escapeCsv("line1\nline2"))
    }

    @Test
    fun `escapeCsv wraps in quotes when value contains carriage return`() {
        assertEquals("\"line1\rline2\"", CredentialExporter.escapeCsv("line1\rline2"))
    }

    @Test
    fun `escapeCsv handles empty string`() {
        assertEquals("", CredentialExporter.escapeCsv(""))
    }

    @Test
    fun `escapeCsv handles value with comma and quote`() {
        assertEquals("\"a,\"\"b\"\"\"", CredentialExporter.escapeCsv("a,\"b\""))
    }

    // --- parseCsvLine tests ---

    @Test
    fun `parseCsvLine splits simple line`() {
        val result = CredentialExporter.parseCsvLine("google.com,user@gmail.com,mypassword")
        assertEquals(3, result.size)
        assertEquals("google.com", result[0])
        assertEquals("user@gmail.com", result[1])
        assertEquals("mypassword", result[2])
    }

    @Test
    fun `parseCsvLine handles quoted fields with commas`() {
        val result = CredentialExporter.parseCsvLine("\"hello,world\",user,pass")
        assertEquals(3, result.size)
        assertEquals("hello,world", result[0])
        assertEquals("user", result[1])
        assertEquals("pass", result[2])
    }

    @Test
    fun `parseCsvLine handles escaped quotes inside quoted field`() {
        val result = CredentialExporter.parseCsvLine("\"say \"\"hello\"\"\",user,pass")
        assertEquals(3, result.size)
        assertEquals("say \"hello\"", result[0])
        assertEquals("user", result[1])
        assertEquals("pass", result[2])
    }

    @Test
    fun `parseCsvLine handles empty fields`() {
        val result = CredentialExporter.parseCsvLine(",,")
        assertEquals(3, result.size)
        assertEquals("", result[0])
        assertEquals("", result[1])
        assertEquals("", result[2])
    }

    @Test
    fun `parseCsvLine handles single field`() {
        val result = CredentialExporter.parseCsvLine("onlyfield")
        assertEquals(1, result.size)
        assertEquals("onlyfield", result[0])
    }

    @Test
    fun `parseCsvLine handles quoted field with no special chars`() {
        val result = CredentialExporter.parseCsvLine("\"normal\",user,pass")
        assertEquals(3, result.size)
        assertEquals("normal", result[0])
    }

    @Test
    fun `parseCsvLine roundtrip with escapeCsv`() {
        val original = listOf("example.com", "user@test.com", "p@ss,w\"ord\n123")
        val csvLine = original.joinToString(",") { CredentialExporter.escapeCsv(it) }
        val parsed = CredentialExporter.parseCsvLine(csvLine)
        assertEquals(original, parsed)
    }

    @Test
    fun `parseCsvLine handles password with special characters`() {
        val result = CredentialExporter.parseCsvLine("site.com,admin,\"p@ss!#\$%^&*()\"")
        assertEquals(3, result.size)
        assertEquals("site.com", result[0])
        assertEquals("admin", result[1])
        assertEquals("p@ss!#\$%^&*()", result[2])
    }
}
