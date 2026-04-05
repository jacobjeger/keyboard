package com.megalife.ime.feature

/**
 * Calculates the gematria (numerical value) of Hebrew text.
 * Standard values: א=1, ב=2, ... ת=400. Final forms have same value as base.
 */
object GematriaCalculator {

    private val values = mapOf(
        'א' to 1, 'ב' to 2, 'ג' to 3, 'ד' to 4, 'ה' to 5,
        'ו' to 6, 'ז' to 7, 'ח' to 8, 'ט' to 9, 'י' to 10,
        'כ' to 20, 'ך' to 20, 'ל' to 30, 'מ' to 40, 'ם' to 40,
        'נ' to 50, 'ן' to 50, 'ס' to 60, 'ע' to 70, 'פ' to 80,
        'ף' to 80, 'צ' to 90, 'ץ' to 90, 'ק' to 100, 'ר' to 200,
        'ש' to 300, 'ת' to 400
    )

    /** Calculate the gematria value of a Hebrew word or phrase */
    fun calculate(text: String): Int {
        return text.sumOf { values[it] ?: 0 }
    }

    /** Check if text contains Hebrew characters */
    fun containsHebrew(text: String): Boolean {
        return text.any { it in values }
    }

    /** Format gematria for display: "שלום [376]" */
    fun formatDisplay(word: String): String {
        val value = calculate(word)
        return if (value > 0) "[$value]" else ""
    }

    /** Mispar Katan -- reduce each letter to single digit */
    fun misparKatan(text: String): Int {
        return text.sumOf { ch ->
            var v = values[ch] ?: 0
            while (v >= 10) v = v.toString().sumOf { it.digitToInt() }
            v
        }
    }

    /** Mispar Kolel -- standard value + number of letters */
    fun misparKolel(text: String): Int {
        return calculate(text) + text.count { it in values }
    }

    /** Mispar Gadol -- final forms get higher values (500-900) */
    fun misparGadol(text: String): Int {
        val gadolValues = values.toMutableMap()
        gadolValues['\u05DA'] = 500  // ך
        gadolValues['\u05DD'] = 600  // ם
        gadolValues['\u05DF'] = 700  // ן
        gadolValues['\u05E3'] = 800  // ף
        gadolValues['\u05E5'] = 900  // ץ
        return text.sumOf { gadolValues[it] ?: 0 }
    }

    /** At-Bash cipher value (alef<->tav, bet<->shin, etc) */
    fun atBash(text: String): Int {
        val letters = "אבגדהוזחטיכלמנסעפצקרשת"
        val reversed = letters.reversed()
        return text.sumOf { ch ->
            val idx = letters.indexOf(ch)
            if (idx >= 0) values[reversed[idx]] ?: 0
            else values[ch] ?: 0 // finals etc. use standard value
        }
    }

    /** Format with method name */
    fun formatDisplayWithMethod(word: String, method: String): String {
        val value = when (method) {
            "standard" -> calculate(word)
            "katan" -> misparKatan(word)
            "kolel" -> misparKolel(word)
            "gadol" -> misparGadol(word)
            "atbash" -> atBash(word)
            else -> calculate(word)
        }
        val methodLabel = when (method) {
            "standard" -> ""
            "katan" -> " \u05E7\u05D8\u05DF"
            "kolel" -> " \u05DB\u05D5\u05DC\u05DC"
            "gadol" -> " \u05D2\u05D3\u05D5\u05DC"
            "atbash" -> " \u05D0\u05EA\u05D1\"\u05E9"
            else -> ""
        }
        return if (value > 0) "[$value$methodLabel]" else ""
    }
}
