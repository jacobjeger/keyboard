package com.megalife.ime.feature

/**
 * Manages Hebrew nikud (vowel points) insertion.
 * Nikud are Unicode combining marks that attach to the preceding consonant.
 */
object NikudManager {

    data class NikudChar(
        val name: String,
        val hebrewName: String,
        val unicode: Char
    )

    /** All nikud characters organized for the panel */
    val allNikud = listOf(
        NikudChar("Patach", "פתח", '\u05B7'),
        NikudChar("Kamatz", "קמץ", '\u05B8'),
        NikudChar("Tzere", "צירה", '\u05B5'),
        NikudChar("Segol", "סגול", '\u05B6'),
        NikudChar("Chirik", "חיריק", '\u05B4'),
        NikudChar("Cholam", "חולם", '\u05B9'),
        NikudChar("Kubutz", "קובוץ", '\u05BB'),
        NikudChar("Shva", "שוא", '\u05B0'),
        NikudChar("Dagesh", "דגש", '\u05BC'),
        NikudChar("Rafe", "רפה", '\u05BF'),
        NikudChar("Chataf Segol", "חטף סגול", '\u05B1'),
        NikudChar("Chataf Patach", "חטף פתח", '\u05B2'),
        NikudChar("Chataf Kamatz", "חטף קמץ", '\u05B3'),
        NikudChar("Shin Dot", "שין ימנית", '\u05C1'),
        NikudChar("Sin Dot", "שין שמאלית", '\u05C2'),
    )

    /** Get nikud for popup when long-pressing a Hebrew letter */
    fun getNikudForPopup(): List<Char> {
        return allNikud.map { it.unicode }
    }
}
