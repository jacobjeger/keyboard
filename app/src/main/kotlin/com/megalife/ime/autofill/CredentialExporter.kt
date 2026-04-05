package com.megalife.ime.autofill

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Import/export credentials in CSV format compatible with Chrome/Firefox password exports.
 * Format: url,username,password
 */
class CredentialExporter(private val repository: CredentialRepository) {

    /**
     * Export all saved credentials to a CSV file.
     * @return The file containing the exported credentials.
     */
    suspend fun exportToCsv(context: Context): File {
        val creds = repository.getAll()
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val file = File(dir, "megalife_passwords.csv")
        file.bufferedWriter().use { writer ->
            writer.write("url,username,password\n")
            for (c in creds) {
                writer.write("${escapeCsv(c.domain)},${escapeCsv(c.username)},${escapeCsv(c.password)}\n")
            }
        }
        return file
    }

    /**
     * Import credentials from a CSV file.
     * Expects header row: url,username,password (or name,url,username,password for Chrome format).
     * @return The number of credentials imported.
     */
    suspend fun importFromCsv(file: File): Int {
        var count = 0
        val lines = file.bufferedReader().use { reader ->
            reader.readLine() // skip header
            reader.readLines()
        }
        for (line in lines) {
            if (line.isNotBlank()) {
                val parts = parseCsvLine(line)
                if (parts.size >= 3) {
                    repository.save(parts[0], parts[1], parts[2])
                    count++
                }
            }
        }
        return count
    }

    companion object {
        /**
         * Escape a value for CSV output. Wraps in quotes if the value contains
         * commas, quotes, or newlines. Doubles any internal quotes.
         */
        fun escapeCsv(value: String): String {
            return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }

        /**
         * Parse a single CSV line into fields, respecting quoted fields that may
         * contain commas, escaped quotes (""), and other special characters.
         */
        fun parseCsvLine(line: String): List<String> {
            val fields = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            var i = 0

            while (i < line.length) {
                val ch = line[i]
                when {
                    inQuotes -> {
                        if (ch == '"') {
                            // Check for escaped quote ""
                            if (i + 1 < line.length && line[i + 1] == '"') {
                                current.append('"')
                                i++ // skip the second quote
                            } else {
                                inQuotes = false
                            }
                        } else {
                            current.append(ch)
                        }
                    }
                    ch == '"' -> {
                        inQuotes = true
                    }
                    ch == ',' -> {
                        fields.add(current.toString())
                        current.clear()
                    }
                    else -> {
                        current.append(ch)
                    }
                }
                i++
            }
            fields.add(current.toString())
            return fields
        }
    }
}
