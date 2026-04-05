package com.megalife.ime.language.locale

import com.megalife.ime.language.*
import java.util.Locale

object HebrewConfig {

    val t9KeyMap = T9KeyMap(
        keyChars = mapOf(
            2 to listOf('א', 'ב', 'ג'),
            3 to listOf('ד', 'ה', 'ו'),
            4 to listOf('ז', 'ח', 'ט'),
            5 to listOf('י', 'כ', 'ל'),
            6 to listOf('מ', 'נ', 'ס'),
            7 to listOf('ע', 'פ', 'צ'),
            8 to listOf('ק', 'ר', 'ש'),
            9 to listOf('ת')
        ),
        key1Chars = listOf('.', ',', '!', '?', '\'', '-', ':', ';', '"'),
        key0Chars = listOf(' '),
        // Map final forms to their base form's key
        extraCharMappings = mapOf(
            'ך' to 'כ',  // final kaf → kaf (key 5)
            'ם' to 'מ',  // final mem → mem (key 6)
            'ן' to 'נ',  // final nun → nun (key 6)
            'ף' to 'פ',  // final pe → pe (key 7)
            'ץ' to 'צ'   // final tsade → tsade (key 7)
        )
    )

    /** Hebrew final forms: base letter -> final form */
    val finalForms = mapOf(
        'כ' to 'ך',
        'מ' to 'ם',
        'נ' to 'ן',
        'פ' to 'ף',
        'צ' to 'ץ'
    )

    val touchLayout = TouchLayout(
        rows = listOf(
            // Row 1 (RTL): / ' ק ר א ט ו ן ם פ
            KeyRow(listOf(
                KeyDef("/"), KeyDef("ף"), KeyDef("ך"),
                KeyDef("ל"), KeyDef("ח"), KeyDef("י"),
                KeyDef("ע"), KeyDef("כ"), KeyDef("ג"),
                KeyDef("ד"), KeyDef("ש")
            )),
            // Row 2: ת ש ר ק צ פ ס נ מ
            KeyRow(
                keys = listOf(
                    KeyDef("ת"), KeyDef("ש"), KeyDef("ר"),
                    KeyDef("ק"), KeyDef("צ"), KeyDef("פ"),
                    KeyDef("ס"), KeyDef("נ"), KeyDef("מ")
                ),
                leftPadding = 0.5f
            ),
            // Row 3: ⇧ ם ן ו ט ז ג ד ב ⌫
            KeyRow(listOf(
                KeyDef("⇧", type = KeyType.SHIFT, widthWeight = 1.3f),
                KeyDef("ם"), KeyDef("ן"), KeyDef("ו"),
                KeyDef("ט"), KeyDef("ז"), KeyDef("ה"),
                KeyDef("ב"), KeyDef("א"),
                KeyDef("⌫", type = KeyType.BACKSPACE, widthWeight = 1.3f)
            )),
            // Row 4: 123 😊 [lang] [space] . ↵
            KeyRow(listOf(
                KeyDef("123", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef("\uD83C\uDF10", type = KeyType.LANGUAGE),
                KeyDef(" ", label = "עברית", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef(".", type = KeyType.PERIOD, popupChars = listOf(".", ",", "!", "?", ";", ":", "׳", "״")),
                KeyDef("↵", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        ),
        isRtl = true
    )

    val config = LanguageConfig(
        code = "he",
        locale = Locale("he", "IL"),
        displayName = "Hebrew",
        nativeDisplayName = "עברית",
        scriptType = ScriptType.HEBREW,
        textDirection = TextDirection.RTL,
        t9KeyMap = t9KeyMap,
        touchLayout = touchLayout,
        dictionaryAsset = "dict_he.txt",
        hasGlideSupport = true,
        hasAutoCorrect = true,
        hasSpellCheck = true,
        finalForms = finalForms,
        voiceLocale = "he-IL"
    )
}
