package com.megalife.ime.language.locale

import com.megalife.ime.language.*
import java.util.Locale

object YiddishConfig {

    val t9KeyMap = T9KeyMap(
        keyChars = mapOf(
            2 to listOf('\u05D0', '\u05D1', '\u05D2'),             // א ב ג
            3 to listOf('\u05D3', '\u05D4', '\u05D5'),             // ד ה ו
            4 to listOf('\u05D6', '\u05D7', '\u05D8'),             // ז ח ט
            5 to listOf('\u05D9', '\u05DB', '\u05DC'),             // י כ ל
            6 to listOf('\u05DE', '\u05E0', '\u05E1'),             // מ נ ס
            7 to listOf('\u05E2', '\u05E4', '\u05E6'),             // ע פ צ
            8 to listOf('\u05E7', '\u05E8', '\u05E9'),             // ק ר ש
            9 to listOf('\u05EA')                                   // ת
        ),
        key1Chars = listOf('.', ',', '!', '?', '\'', '-', ':', ';', '"'),
        key0Chars = listOf(' '),
        extraCharMappings = mapOf(
            '\u05DA' to '\u05DB',  // final kaf -> kaf (key 5)
            '\u05DD' to '\u05DE',  // final mem -> mem (key 6)
            '\u05DF' to '\u05E0',  // final nun -> nun (key 6)
            '\u05E3' to '\u05E4',  // final pe -> pe (key 7)
            '\u05E5' to '\u05E6'   // final tsade -> tsade (key 7)
        )
    )

    /** Hebrew final forms used in Yiddish: base letter -> final form */
    val finalForms = mapOf(
        '\u05DB' to '\u05DA',  // kaf -> final kaf
        '\u05DE' to '\u05DD',  // mem -> final mem
        '\u05E0' to '\u05DF',  // nun -> final nun
        '\u05E4' to '\u05E3',  // pe -> final pe
        '\u05E6' to '\u05E5'   // tsade -> final tsade
    )

    val touchLayout = TouchLayout(
        rows = listOf(
            // Row 1 (RTL): Same Hebrew layout with Yiddish-specific popups
            KeyRow(listOf(
                KeyDef("/"), KeyDef("\u05E3"), KeyDef("\u05DA"),
                KeyDef("\u05DC"), KeyDef("\u05D7"), KeyDef("\u05D9"),
                KeyDef("\u05E2"), KeyDef("\u05DB"), KeyDef("\u05D2"),
                KeyDef("\u05D3"), KeyDef("\u05E9")
            )),
            // Row 2
            KeyRow(
                keys = listOf(
                    KeyDef("\u05EA"), KeyDef("\u05E9"), KeyDef("\u05E8"),
                    KeyDef("\u05E7"), KeyDef("\u05E6"), KeyDef("\u05E4"),
                    KeyDef("\u05E1"), KeyDef("\u05E0"), KeyDef("\u05DE")
                ),
                leftPadding = 0.5f
            ),
            // Row 3: ⇧ ם ן ו ט ז ה ב א ⌫ — with Yiddish digraph popups
            KeyRow(listOf(
                KeyDef("\u21E7", type = KeyType.SHIFT, widthWeight = 1.3f),
                KeyDef("\u05DD"), KeyDef("\u05DF"),
                KeyDef("\u05D5", popupChars = listOf("\u05D5\u05D5", "\u05D5\u05B4")),
                KeyDef("\u05D8"), KeyDef("\u05D6"),
                KeyDef("\u05D4"), KeyDef("\u05D1"),
                KeyDef("\u05D0", popupChars = listOf("\u05D0\u05B7", "\u05D0\u05B8")),
                KeyDef("\u232B", type = KeyType.BACKSPACE, widthWeight = 1.3f)
            )),
            // Row 4: 123 emoji [lang] [space] . enter
            KeyRow(listOf(
                KeyDef("123", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef("\uD83C\uDF10", type = KeyType.LANGUAGE),
                KeyDef(" ", label = "\u05D9\u05D9\u05B4\u05D3\u05D9\u05E9", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef(".", type = KeyType.PERIOD, popupChars = listOf(".", ",", "!", "?", ";", ":", "\u05F3", "\u05F4")),
                KeyDef("\u21B5", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        ),
        isRtl = true
    )

    val config = LanguageConfig(
        code = "yi",
        locale = Locale("yi"),
        displayName = "Yiddish",
        nativeDisplayName = "\u05D9\u05D9\u05B4\u05D3\u05D9\u05E9",
        scriptType = ScriptType.HEBREW,
        textDirection = TextDirection.RTL,
        t9KeyMap = t9KeyMap,
        touchLayout = touchLayout,
        dictionaryAsset = "dict_yi.txt",
        finalForms = finalForms,
        voiceLocale = "yi"
    )
}
