package com.megalife.ime.language.locale

import com.megalife.ime.language.*
import java.util.Locale

object ArabicConfig {

    val t9KeyMap = T9KeyMap(
        keyChars = mapOf(
            2 to listOf('\u0627', '\u0628', '\u062A', '\u062B'),  // ا ب ت ث
            3 to listOf('\u062C', '\u062D', '\u062E'),              // ج ح خ
            4 to listOf('\u062F', '\u0630', '\u0631', '\u0632'),    // د ذ ر ز
            5 to listOf('\u0633', '\u0634', '\u0635', '\u0636'),    // س ش ص ض
            6 to listOf('\u0637', '\u0638', '\u0639', '\u063A'),    // ط ظ ع غ
            7 to listOf('\u0641', '\u0642', '\u0643'),              // ف ق ك
            8 to listOf('\u0644', '\u0645', '\u0646'),              // ل م ن
            9 to listOf('\u0647', '\u0648', '\u064A')               // ه و ي
        ),
        key1Chars = listOf('.', ',', '!', '?', '\u060C', '\u061B', '\u061F'),  // Arabic comma, semicolon, question mark
        key0Chars = listOf(' ')
    )

    val touchLayout = TouchLayout(
        rows = listOf(
            // Row 1: ض ص ث ق ف غ ع ه خ ح ج
            KeyRow(listOf(
                KeyDef("\u0636"), KeyDef("\u0635"), KeyDef("\u062B"),
                KeyDef("\u0642"), KeyDef("\u0641"), KeyDef("\u063A"),
                KeyDef("\u0639"), KeyDef("\u0647"), KeyDef("\u062E"),
                KeyDef("\u062D"), KeyDef("\u062C")
            )),
            // Row 2: ش س ي ب ل ا ت ن م ك
            KeyRow(
                keys = listOf(
                    KeyDef("\u0634"), KeyDef("\u0633"), KeyDef("\u064A"),
                    KeyDef("\u0628"), KeyDef("\u0644"), KeyDef("\u0627", popupChars = listOf("\u0622", "\u0623", "\u0625", "\u0671")),
                    KeyDef("\u062A", popupChars = listOf("\u0629")),
                    KeyDef("\u0646"), KeyDef("\u0645"),
                    KeyDef("\u0643")
                ),
                leftPadding = 0.5f
            ),
            // Row 3: ⇧ ئ ء ؤ ر لا ى ة و ز ⌫
            KeyRow(listOf(
                KeyDef("\u21E7", type = KeyType.SHIFT, widthWeight = 1.3f),
                KeyDef("\u0626"), KeyDef("\u0621"), KeyDef("\u0624"),
                KeyDef("\u0631"), KeyDef("\u0644\u0627"),
                KeyDef("\u0649"), KeyDef("\u0629"),
                KeyDef("\u0648"), KeyDef("\u0632"),
                KeyDef("\u232B", type = KeyType.BACKSPACE, widthWeight = 1.3f)
            )),
            // Row 4: 123 emoji [lang] [space] . enter
            KeyRow(listOf(
                KeyDef("123", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef("\uD83C\uDF10", type = KeyType.LANGUAGE),
                KeyDef(" ", label = "\u0627\u0644\u0639\u0631\u0628\u064A\u0629", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef(".", type = KeyType.PERIOD, popupChars = listOf(".", "\u060C", "!", "?", "\u061F", "\u061B", ":", "'", "\"", "-")),
                KeyDef("\u21B5", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        ),
        isRtl = true
    )

    val config = LanguageConfig(
        code = "ar",
        locale = Locale("ar", "SA"),
        displayName = "Arabic",
        nativeDisplayName = "\u0627\u0644\u0639\u0631\u0628\u064A\u0629",
        scriptType = ScriptType.ARABIC,
        textDirection = TextDirection.RTL,
        t9KeyMap = t9KeyMap,
        touchLayout = touchLayout,
        dictionaryAsset = "dict_ar.txt",
        voiceLocale = "ar-SA"
    )
}
