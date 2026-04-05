package com.megalife.ime.language.locale

import com.megalife.ime.language.*
import java.util.Locale

object FrenchConfig {

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

    // French AZERTY layout
    val touchLayout = TouchLayout(
        rows = listOf(
            // Row 1: A Z E R T Y U I O P
            KeyRow(listOf(
                KeyDef("a", popupChars = listOf("\u00E0", "\u00E2", "\u00E6", "\u00E4")),
                KeyDef("z"),
                KeyDef("e", popupChars = listOf("\u00E9", "\u00E8", "\u00EA", "\u00EB")),
                KeyDef("r"),
                KeyDef("t"),
                KeyDef("y", popupChars = listOf("\u00FF")),
                KeyDef("u", popupChars = listOf("\u00F9", "\u00FB", "\u00FC")),
                KeyDef("i", popupChars = listOf("\u00EE", "\u00EF")),
                KeyDef("o", popupChars = listOf("\u00F4", "\u0153", "\u00F6")),
                KeyDef("p")
            )),
            // Row 2: Q S D F G H J K L M
            KeyRow(
                keys = listOf(
                    KeyDef("q"),
                    KeyDef("s"),
                    KeyDef("d"),
                    KeyDef("f"),
                    KeyDef("g"),
                    KeyDef("h"),
                    KeyDef("j"),
                    KeyDef("k"),
                    KeyDef("l"),
                    KeyDef("m")
                ),
                leftPadding = 0.3f
            ),
            // Row 3: ⇧ W X C V B N ⌫
            KeyRow(listOf(
                KeyDef("\u21E7", type = KeyType.SHIFT, widthWeight = 1.5f),
                KeyDef("w"),
                KeyDef("x"),
                KeyDef("c", popupChars = listOf("\u00E7")),
                KeyDef("v"),
                KeyDef("b"),
                KeyDef("n", popupChars = listOf("\u00F1")),
                KeyDef("\u232B", type = KeyType.BACKSPACE, widthWeight = 1.5f)
            )),
            // Row 4: 123 emoji [lang] [space] . enter
            KeyRow(listOf(
                KeyDef("123", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef("\uD83C\uDF10", type = KeyType.LANGUAGE),
                KeyDef(" ", label = "Fran\u00E7ais", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef(".", type = KeyType.PERIOD, popupChars = listOf(".", ",", "!", "?", ";", ":", "'", "\"", "-", "\u2026", "\u00AB", "\u00BB")),
                KeyDef("\u21B5", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        ),
        isRtl = false
    )

    val config = LanguageConfig(
        code = "fr",
        locale = Locale.FRANCE,
        displayName = "French",
        nativeDisplayName = "Fran\u00E7ais",
        scriptType = ScriptType.LATIN,
        textDirection = TextDirection.LTR,
        t9KeyMap = t9KeyMap,
        touchLayout = touchLayout,
        dictionaryAsset = "dict_fr.txt",
        voiceLocale = "fr-FR"
    )
}
