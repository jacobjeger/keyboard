package com.megalife.ime.language.locale

import com.megalife.ime.language.*
import java.util.Locale

object EnglishConfig {

    val t9KeyMap = T9KeyMap(
        keyChars = mapOf(
            2 to listOf('a', 'b', 'c'),
            3 to listOf('d', 'e', 'f'),
            4 to listOf('g', 'h', 'i'),
            5 to listOf('j', 'k', 'l'),
            6 to listOf('m', 'n', 'o'),
            7 to listOf('p', 'q', 'r', 's'),
            8 to listOf('t', 'u', 'v'),
            9 to listOf('w', 'x', 'y', 'z')
        )
    )

    val touchLayout = TouchLayout(
        rows = listOf(
            // Row 1: Q W E R T Y U I O P
            KeyRow(listOf(
                KeyDef("q"), KeyDef("w"), KeyDef("e", popupChars = listOf("è", "é", "ê", "ë")),
                KeyDef("r"), KeyDef("t"), KeyDef("y"),
                KeyDef("u", popupChars = listOf("ù", "ú", "û", "ü")),
                KeyDef("i", popupChars = listOf("ì", "í", "î", "ï")),
                KeyDef("o", popupChars = listOf("ò", "ó", "ô", "ö")),
                KeyDef("p")
            )),
            // Row 2: A S D F G H J K L
            KeyRow(
                keys = listOf(
                    KeyDef("a", popupChars = listOf("à", "á", "â", "ä", "å")),
                    KeyDef("s", popupChars = listOf("ß")),
                    KeyDef("d"), KeyDef("f"), KeyDef("g"), KeyDef("h"),
                    KeyDef("j"), KeyDef("k"), KeyDef("l")
                ),
                leftPadding = 0.5f
            ),
            // Row 3: ⇧ Z X C V B N M ⌫
            KeyRow(listOf(
                KeyDef("⇧", type = KeyType.SHIFT, widthWeight = 1.5f),
                KeyDef("z"), KeyDef("x"),
                KeyDef("c", popupChars = listOf("ç")),
                KeyDef("v"), KeyDef("b"),
                KeyDef("n", popupChars = listOf("ñ")),
                KeyDef("m"),
                KeyDef("⌫", type = KeyType.BACKSPACE, widthWeight = 1.5f)
            )),
            // Row 4: 123 😊 [lang] [space] . ↵
            KeyRow(listOf(
                KeyDef("123", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef("\uD83C\uDF10", type = KeyType.LANGUAGE),
                KeyDef(" ", label = "English", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef(".", type = KeyType.PERIOD, popupChars = listOf(".", ",", "!", "?", ";", ":", "'", "\"", "-", "…", "—")),
                KeyDef("↵", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        ),
        isRtl = false
    )

    val config = LanguageConfig(
        code = "en",
        locale = Locale.US,
        displayName = "English",
        nativeDisplayName = "English",
        scriptType = ScriptType.LATIN,
        textDirection = TextDirection.LTR,
        t9KeyMap = t9KeyMap,
        touchLayout = touchLayout,
        dictionaryAsset = "dict_en.txt",
        voiceLocale = "en-US"
    )
}
