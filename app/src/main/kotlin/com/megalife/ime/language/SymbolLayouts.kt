package com.megalife.ime.language

/**
 * Symbol and number layouts for the on-screen keyboard.
 * Two pages: symbols1 (numbers + common symbols), symbols2 (more symbols).
 */
object SymbolLayouts {

    val symbolsPage1 = TouchLayout(
        rows = listOf(
            // Row 1: 1 2 3 4 5 6 7 8 9 0
            KeyRow(listOf(
                KeyDef("1"), KeyDef("2"), KeyDef("3"), KeyDef("4"), KeyDef("5"),
                KeyDef("6"), KeyDef("7"), KeyDef("8"), KeyDef("9"), KeyDef("0")
            )),
            // Row 2: @ # $ % & - + ( )
            KeyRow(listOf(
                KeyDef("@"), KeyDef("#"), KeyDef("$", popupChars = listOf("€", "£", "¥", "₪")),
                KeyDef("%"), KeyDef("&"), KeyDef("-", popupChars = listOf("–", "—", "~")),
                KeyDef("+"), KeyDef("("), KeyDef(")")
            )),
            // Row 3: [more] = * " ' : ; ! ? [backspace]
            KeyRow(listOf(
                KeyDef("1/2", label = "1/2", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                KeyDef("="), KeyDef("*"),
                KeyDef("\""), KeyDef("'", popupChars = listOf("'", "'", "‛")),
                KeyDef(":"), KeyDef(";"),
                KeyDef("!"), KeyDef("?"),
                KeyDef("⌫", type = KeyType.BACKSPACE, widthWeight = 1.5f)
            )),
            // Row 4: ABC [emoji] [space] . [enter]
            KeyRow(listOf(
                KeyDef("ABC", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef(" ", label = " ", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef(".", type = KeyType.PERIOD, popupChars = listOf(",", "…", "/", "\\", "|")),
                KeyDef("↵", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        )
    )

    val symbolsPage2 = TouchLayout(
        rows = listOf(
            // Row 1: ~ ` | · √ π ÷ × ¶ ∆
            KeyRow(listOf(
                KeyDef("~"), KeyDef("`"), KeyDef("|"), KeyDef("·"), KeyDef("√"),
                KeyDef("π"), KeyDef("÷"), KeyDef("×"), KeyDef("¶"), KeyDef("∆")
            )),
            // Row 2: £ € ¥ ₪ ^ ° = { }
            KeyRow(listOf(
                KeyDef("£"), KeyDef("€"), KeyDef("¥"), KeyDef("₪"),
                KeyDef("^"), KeyDef("°"),
                KeyDef("="), KeyDef("{"), KeyDef("}")
            )),
            // Row 3: [more] \ / [ ] < > « » [backspace]
            KeyRow(listOf(
                KeyDef("2/2", label = "2/2", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                KeyDef("\\"), KeyDef("/"),
                KeyDef("["), KeyDef("]"),
                KeyDef("<"), KeyDef(">"),
                KeyDef("«"), KeyDef("»"),
                KeyDef("⌫", type = KeyType.BACKSPACE, widthWeight = 1.5f)
            )),
            // Row 4: ABC [emoji] [space] . [enter]
            KeyRow(listOf(
                KeyDef("ABC", type = KeyType.SYMBOLS, widthWeight = 1.2f),
                KeyDef("\uD83D\uDE0A", type = KeyType.EMOJI),
                KeyDef(" ", label = " ", type = KeyType.SPACE, widthWeight = 4f),
                KeyDef(".", type = KeyType.PERIOD),
                KeyDef("↵", type = KeyType.ENTER, widthWeight = 1.3f)
            ))
        )
    )

    /** Number pad for TYPE_CLASS_NUMBER fields */
    val numberPad = TouchLayout(
        rows = listOf(
            KeyRow(listOf(KeyDef("1"), KeyDef("2"), KeyDef("3"))),
            KeyRow(listOf(KeyDef("4"), KeyDef("5"), KeyDef("6"))),
            KeyRow(listOf(KeyDef("7"), KeyDef("8"), KeyDef("9"))),
            KeyRow(listOf(
                KeyDef("-"), KeyDef("0"), KeyDef(".", type = KeyType.PERIOD)
            )),
            KeyRow(listOf(
                KeyDef("⌫", type = KeyType.BACKSPACE, widthWeight = 1.5f),
                KeyDef(" ", label = " ", type = KeyType.SPACE, widthWeight = 1.5f),
                KeyDef("↵", type = KeyType.ENTER, widthWeight = 1.5f)
            ))
        )
    )

    /** Phone pad for TYPE_CLASS_PHONE fields */
    val phonePad = TouchLayout(
        rows = listOf(
            KeyRow(listOf(
                KeyDef("1", subLabel = ""), KeyDef("2", subLabel = "ABC"), KeyDef("3", subLabel = "DEF")
            )),
            KeyRow(listOf(
                KeyDef("4", subLabel = "GHI"), KeyDef("5", subLabel = "JKL"), KeyDef("6", subLabel = "MNO")
            )),
            KeyRow(listOf(
                KeyDef("7", subLabel = "PQRS"), KeyDef("8", subLabel = "TUV"), KeyDef("9", subLabel = "WXYZ")
            )),
            KeyRow(listOf(
                KeyDef("*"), KeyDef("0", subLabel = "+"), KeyDef("#")
            )),
            KeyRow(listOf(
                KeyDef("⌫", type = KeyType.BACKSPACE, widthWeight = 1.5f),
                KeyDef(" ", label = " ", type = KeyType.SPACE, widthWeight = 1.5f),
                KeyDef("↵", type = KeyType.ENTER, widthWeight = 1.5f)
            ))
        )
    )
}
