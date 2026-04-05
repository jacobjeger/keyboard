package com.megalife.ime.language.locale

import com.megalife.ime.language.*
import java.util.Locale

object AmharicConfig {

    // Amharic does not use T9 — provide an empty keymap as placeholder
    val t9KeyMap = T9KeyMap(
        keyChars = emptyMap()
    )

    // Amharic Ge'ez syllabary layout — 4 rows of common characters
    val touchLayout = TouchLayout(
        rows = listOf(
            // Row 1: Common Ge'ez base characters (first order forms)
            KeyRow(listOf(
                KeyDef("\u1200", popupChars = listOf("\u1201", "\u1202", "\u1203", "\u1204", "\u1205", "\u1206")),  // ሀ + orders
                KeyDef("\u1208", popupChars = listOf("\u1209", "\u120A", "\u120B", "\u120C", "\u120D", "\u120E")),  // ለ
                KeyDef("\u1218", popupChars = listOf("\u1219", "\u121A", "\u121B", "\u121C", "\u121D", "\u121E")),  // መ
                KeyDef("\u1228", popupChars = listOf("\u1229", "\u122A", "\u122B", "\u122C", "\u122D", "\u122E")),  // ረ
                KeyDef("\u1230", popupChars = listOf("\u1231", "\u1232", "\u1233", "\u1234", "\u1235", "\u1236")),  // ሰ
                KeyDef("\u1240", popupChars = listOf("\u1241", "\u1242", "\u1243", "\u1244", "\u1245", "\u1246")),  // ቀ
                KeyDef("\u1260", popupChars = listOf("\u1261", "\u1262", "\u1263", "\u1264", "\u1265", "\u1266")),  // በ
                KeyDef("\u1270", popupChars = listOf("\u1271", "\u1272", "\u1273", "\u1274", "\u1275", "\u1276")),  // ተ
                KeyDef("\u1290", popupChars = listOf("\u1291", "\u1292", "\u1293", "\u1294", "\u1295", "\u1296"))   // ነ
            )),
            // Row 2: More common characters
            KeyRow(
                keys = listOf(
                    KeyDef("\u12A0", popupChars = listOf("\u12A1", "\u12A2", "\u12A3", "\u12A4", "\u12A5", "\u12A6")),  // አ
                    KeyDef("\u12A8", popupChars = listOf("\u12A9", "\u12AA", "\u12AB", "\u12AC", "\u12AD", "\u12AE")),  // ከ
                    KeyDef("\u12C8", popupChars = listOf("\u12C9", "\u12CA", "\u12CB", "\u12CC", "\u12CD", "\u12CE")),  // ወ
                    KeyDef("\u12D8", popupChars = listOf("\u12D9", "\u12DA", "\u12DB", "\u12DC", "\u12DD", "\u12DE")),  // ዘ
                    KeyDef("\u12E8", popupChars = listOf("\u12E9", "\u12EA", "\u12EB", "\u12EC", "\u12ED", "\u12EE")),  // የ
                    KeyDef("\u12F0", popupChars = listOf("\u12F1", "\u12F2", "\u12F3", "\u12F4", "\u12F5", "\u12F6")),  // ደ
                    KeyDef("\u1300", popupChars = listOf("\u1301", "\u1302", "\u1303", "\u1304", "\u1305", "\u1306")),  // ጀ
                    KeyDef("\u1308", popupChars = listOf("\u1309", "\u130A", "\u130B", "\u130C", "\u130D", "\u130E"))   // ገ
                ),
                leftPadding = 0.5f
            ),
            // Row 3: Less common + shift/backspace
            KeyRow(listOf(
                KeyDef("\u21E7", type = KeyType.SHIFT, widthWeight = 1.3f),
                KeyDef("\u1320", popupChars = listOf("\u1321", "\u1322", "\u1323", "\u1324", "\u1325", "\u1326")),  // ጠ
                KeyDef("\u1328", popupChars = listOf("\u1329", "\u132A", "\u132B", "\u132C", "\u132D", "\u132E")),  // ጨ
                KeyDef("\u1330", popupChars = listOf("\u1331", "\u1332", "\u1333", "\u1334", "\u1335", "\u1336")),  // ጰ
                KeyDef("\u1338", popupChars = listOf("\u1339", "\u133A", "\u133B", "\u133C", "\u133D", "\u133E")),  // ጸ
                KeyDef("\u1340", popupChars = listOf("\u1341", "\u1342", "\u1343", "\u1344", "\u1345", "\u1346")),  // ፀ
                KeyDef("\u1348", popupChars = listOf("\u1349", "\u134A", "\u134B", "\u134C", "\u134D", "\u134E")),  // ፈ
                KeyDef("\u1350", popupChars = listOf("\u1351", "\u1352", "\u1353", "\u1354", "\u1355", "\u1356")),  // ፐ
                KeyDef("\u232B", type = KeyType.BACKSPACE, widthWeight = 1.3f)
            )),
            // Row 4: 123 emoji [lang] [space] ። enter
            KeyRow(listOf(
                KeyDef("123", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef("\uD83C\uDF10", type = KeyType.LANGUAGE),
                KeyDef(" ", label = "\u12A0\u121B\u122D\u129B", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef("\u1362", type = KeyType.PERIOD, popupChars = listOf("\u1362", "\u1363", "\u1364", "\u1365", "\u1366", "\u1367")),
                KeyDef("\u21B5", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        ),
        isRtl = false
    )

    val config = LanguageConfig(
        code = "am",
        locale = Locale("am", "ET"),
        displayName = "Amharic",
        nativeDisplayName = "\u12A0\u121B\u122D\u129B",
        scriptType = ScriptType.GEEZ,
        textDirection = TextDirection.LTR,
        t9KeyMap = t9KeyMap,
        touchLayout = touchLayout,
        dictionaryAsset = "dict_am.txt",
        hasT9Support = false,
        voiceLocale = "am-ET"
    )
}
