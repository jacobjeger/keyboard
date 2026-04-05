package com.megalife.ime.language.locale

import com.megalife.ime.language.*
import java.util.Locale

object RussianConfig {

    val t9KeyMap = T9KeyMap(
        keyChars = mapOf(
            2 to listOf('\u0430', '\u0431', '\u0432', '\u0433'),    // а б в г
            3 to listOf('\u0434', '\u0435', '\u0436', '\u0437'),    // д е ж з
            4 to listOf('\u0438', '\u0439', '\u043A', '\u043B'),    // и й к л
            5 to listOf('\u043C', '\u043D', '\u043E', '\u043F'),    // м н о п
            6 to listOf('\u0440', '\u0441', '\u0442', '\u0443'),    // р с т у
            7 to listOf('\u0444', '\u0445', '\u0446', '\u0447'),    // ф х ц ч
            8 to listOf('\u0448', '\u0449', '\u044A', '\u044B'),    // ш щ ъ ы
            9 to listOf('\u044C', '\u044D', '\u044E', '\u044F')     // ь э ю я
        ),
        key1Chars = listOf('.', ',', '!', '?', '\'', '-', ':', ';', '"'),
        key0Chars = listOf(' ')
    )

    // Standard Russian JCUKEN (ЙЦУКЕН) layout
    val touchLayout = TouchLayout(
        rows = listOf(
            // Row 1: Й Ц У К Е Н Г Ш Щ З Х Ъ
            KeyRow(listOf(
                KeyDef("\u0439"), KeyDef("\u0446"), KeyDef("\u0443"),
                KeyDef("\u043A"), KeyDef("\u0435", popupChars = listOf("\u0451")),
                KeyDef("\u043D"), KeyDef("\u0433"),
                KeyDef("\u0448"), KeyDef("\u0449"),
                KeyDef("\u0437"), KeyDef("\u0445"), KeyDef("\u044A")
            )),
            // Row 2: Ф Ы В А П Р О Л Д Ж Э
            KeyRow(
                keys = listOf(
                    KeyDef("\u0444"), KeyDef("\u044B"), KeyDef("\u0432"),
                    KeyDef("\u0430"), KeyDef("\u043F"), KeyDef("\u0440"),
                    KeyDef("\u043E"), KeyDef("\u043B"), KeyDef("\u0434"),
                    KeyDef("\u0436"), KeyDef("\u044D")
                ),
                leftPadding = 0.3f
            ),
            // Row 3: ⇧ Я Ч С М И Т Ь Б Ю ⌫
            KeyRow(listOf(
                KeyDef("\u21E7", type = KeyType.SHIFT, widthWeight = 1.3f),
                KeyDef("\u044F"), KeyDef("\u0447"), KeyDef("\u0441"),
                KeyDef("\u043C"), KeyDef("\u0438"), KeyDef("\u0442"),
                KeyDef("\u044C"), KeyDef("\u0431"), KeyDef("\u044E"),
                KeyDef("\u232B", type = KeyType.BACKSPACE, widthWeight = 1.3f)
            )),
            // Row 4: 123 emoji [lang] [space] . enter
            KeyRow(listOf(
                KeyDef("123", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef("\uD83C\uDF10", type = KeyType.LANGUAGE),
                KeyDef(" ", label = "\u0420\u0443\u0441\u0441\u043A\u0438\u0439", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef(".", type = KeyType.PERIOD, popupChars = listOf(".", ",", "!", "?", ";", ":", "'", "\"", "-")),
                KeyDef("\u21B5", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        ),
        isRtl = false
    )

    val config = LanguageConfig(
        code = "ru",
        locale = Locale("ru", "RU"),
        displayName = "Russian",
        nativeDisplayName = "\u0420\u0443\u0441\u0441\u043A\u0438\u0439",
        scriptType = ScriptType.CYRILLIC,
        textDirection = TextDirection.LTR,
        t9KeyMap = t9KeyMap,
        touchLayout = touchLayout,
        dictionaryAsset = "dict_ru.txt",
        voiceLocale = "ru-RU"
    )
}
