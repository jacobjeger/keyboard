package com.megalife.ime.feature

/**
 * Suggests nikud (vowel points) for common Hebrew words.
 * Uses a lookup table of common words with canonical nikud.
 */
object NikudSuggester {

    // Map of unpointed Hebrew words to their nikud-annotated versions
    private val nikudMap: Map<String, List<String>> = mapOf(
        // Greetings and common words
        "שלום" to listOf("שָׁלוֹם"),
        "ברוך" to listOf("בָּרוּךְ"),
        "תודה" to listOf("תּוֹדָה"),
        "בבקשה" to listOf("בְּבַקָּשָׁה"),
        "סליחה" to listOf("סְלִיחָה"),
        "כן" to listOf("כֵּן"),
        "לא" to listOf("לֹא"),
        "שמח" to listOf("שָׂמֵחַ"),
        "טוב" to listOf("טוֹב"),

        // Religious and spiritual
        "אלוהים" to listOf("אֱלֹהִים"),
        "ישראל" to listOf("יִשְׂרָאֵל"),
        "תורה" to listOf("תּוֹרָה"),
        "מלך" to listOf("מֶלֶךְ"),
        "ברכה" to listOf("בְּרָכָה"),
        "תפילה" to listOf("תְּפִלָּה"),
        "שבת" to listOf("שַׁבָּת"),
        "חג" to listOf("חַג"),
        "מצוה" to listOf("מִצְוָה"),
        "כהן" to listOf("כֹּהֵן"),
        "נביא" to listOf("נָבִיא"),
        "קדוש" to listOf("קָדוֹשׁ"),
        "אמן" to listOf("אָמֵן"),
        "הלל" to listOf("הַלֵּל"),
        "משיח" to listOf("מָשִׁיחַ"),
        "נשמה" to listOf("נְשָׁמָה"),
        "תשובה" to listOf("תְּשׁוּבָה"),
        "צדיק" to listOf("צַדִּיק"),
        "רבי" to listOf("רַבִּי"),
        "מזוזה" to listOf("מְזוּזָה"),
        "סידור" to listOf("סִדּוּר"),
        "כיפה" to listOf("כִּפָּה"),
        "טלית" to listOf("טַלִּית"),
        "תפילין" to listOf("תְּפִלִּין"),
        "חנוכה" to listOf("חֲנֻכָּה"),
        "פסח" to listOf("פֶּסַח"),
        "סוכה" to listOf("סֻכָּה"),
        "שופר" to listOf("שׁוֹפָר"),
        "מגילה" to listOf("מְגִלָּה"),
        "הגדה" to listOf("הַגָּדָה"),
        "כשר" to listOf("כָּשֵׁר"),

        // Nature and elements
        "ארץ" to listOf("אֶרֶץ"),
        "שמש" to listOf("שֶׁמֶשׁ", "שַׁמָּשׁ"),
        "מים" to listOf("מַיִם"),
        "אש" to listOf("אֵשׁ"),
        "רוח" to listOf("רוּחַ"),
        "שמים" to listOf("שָׁמַיִם"),
        "כוכב" to listOf("כּוֹכָב"),
        "ירח" to listOf("יָרֵחַ"),
        "ים" to listOf("יָם"),
        "הר" to listOf("הַר"),
        "עץ" to listOf("עֵץ"),
        "פרח" to listOf("פֶּרַח"),
        "אבן" to listOf("אֶבֶן"),
        "נהר" to listOf("נָהָר"),
        "גשם" to listOf("גֶּשֶׁם"),
        "שלג" to listOf("שֶׁלֶג"),
        "ענן" to listOf("עָנָן"),
        "אור" to listOf("אוֹר"),
        "חושך" to listOf("חֹשֶׁךְ"),

        // Body parts
        "נפש" to listOf("נֶפֶשׁ"),
        "לב" to listOf("לֵב"),
        "עין" to listOf("עַיִן"),
        "יד" to listOf("יָד"),
        "רגל" to listOf("רֶגֶל"),
        "ראש" to listOf("רֹאשׁ"),
        "פה" to listOf("פֶּה"),
        "אוזן" to listOf("אֹזֶן"),
        "אף" to listOf("אַף"),
        "כתף" to listOf("כָּתֵף"),
        "גוף" to listOf("גּוּף"),
        "דם" to listOf("דָּם"),

        // Family
        "אבא" to listOf("אַבָּא"),
        "אמא" to listOf("אִמָּא"),
        "ילד" to listOf("יֶלֶד"),
        "ילדה" to listOf("יַלְדָּה"),
        "אח" to listOf("אָח"),
        "אחות" to listOf("אָחוֹת"),
        "בן" to listOf("בֵּן"),
        "בת" to listOf("בַּת"),
        "משפחה" to listOf("מִשְׁפָּחָה"),
        "אישה" to listOf("אִשָּׁה"),
        "איש" to listOf("אִישׁ"),
        "סבא" to listOf("סָבָא"),
        "סבתא" to listOf("סָבְתָא"),

        // Abstract concepts
        "דבר" to listOf("דָּבָר", "דִּבֵּר"),
        "עולם" to listOf("עוֹלָם"),
        "חיים" to listOf("חַיִּים"),
        "שנה" to listOf("שָׁנָה"),
        "אמת" to listOf("אֱמֶת"),
        "חסד" to listOf("חֶסֶד"),
        "משפט" to listOf("מִשְׁפָּט"),
        "צדק" to listOf("צֶדֶק"),
        "חכמה" to listOf("חָכְמָה"),
        "בינה" to listOf("בִּינָה"),
        "דעת" to listOf("דַּעַת"),
        "שלם" to listOf("שָׁלֵם"),
        "חדש" to listOf("חָדָשׁ"),
        "ישן" to listOf("יָשָׁן"),
        "גדול" to listOf("גָּדוֹל"),
        "קטן" to listOf("קָטָן"),

        // Time
        "יום" to listOf("יוֹם"),
        "לילה" to listOf("לַיְלָה"),
        "בוקר" to listOf("בֹּקֶר"),
        "ערב" to listOf("עֶרֶב"),
        "שעה" to listOf("שָׁעָה"),
        "דקה" to listOf("דַּקָּה"),
        "חודש" to listOf("חֹדֶשׁ"),
        "שבוע" to listOf("שָׁבוּעַ"),
        "זמן" to listOf("זְמַן"),
        "עתיד" to listOf("עָתִיד"),
        "עבר" to listOf("עָבָר"),

        // Places
        "בית" to listOf("בַּיִת", "בֵּית"),
        "עיר" to listOf("עִיר"),
        "דרך" to listOf("דֶּרֶךְ"),
        "מקום" to listOf("מָקוֹם"),
        "ספר" to listOf("סֵפֶר", "סִפֵּר"),
        "חדר" to listOf("חֶדֶר"),
        "שער" to listOf("שַׁעַר"),
        "גן" to listOf("גַּן"),
        "שוק" to listOf("שׁוּק"),
        "רחוב" to listOf("רְחוֹב"),

        // Common verbs (infinitive/basic form)
        "לכתוב" to listOf("לִכְתֹּב"),
        "לקרוא" to listOf("לִקְרֹא"),
        "ללכת" to listOf("לָלֶכֶת"),
        "לאכול" to listOf("לֶאֱכֹל"),
        "לשתות" to listOf("לִשְׁתּוֹת"),
        "לדבר" to listOf("לְדַבֵּר"),
        "לשמוע" to listOf("לִשְׁמֹעַ"),
        "לראות" to listOf("לִרְאוֹת"),
        "לתת" to listOf("לָתֵת"),
        "לקחת" to listOf("לָקַחַת"),
        "לעשות" to listOf("לַעֲשׂוֹת"),
        "לדעת" to listOf("לָדַעַת"),
        "לבוא" to listOf("לָבוֹא"),
        "לשבת" to listOf("לָשֶׁבֶת"),
        "לעמוד" to listOf("לַעֲמֹד"),

        // Food
        "לחם" to listOf("לֶחֶם"),
        "יין" to listOf("יַיִן"),
        "חלב" to listOf("חָלָב"),
        "דבש" to listOf("דְּבַשׁ"),
        "מלח" to listOf("מֶלַח"),
        "פרי" to listOf("פְּרִי"),
        "בשר" to listOf("בָּשָׂר"),

        // Animals
        "כלב" to listOf("כֶּלֶב"),
        "חתול" to listOf("חָתוּל"),
        "סוס" to listOf("סוּס"),
        "ציפור" to listOf("צִפּוֹר"),
        "דג" to listOf("דָּג"),
        "אריה" to listOf("אַרְיֵה"),
        "נחש" to listOf("נָחָשׁ"),
        "תרנגול" to listOf("תַּרְנְגוֹל"),

        // Numbers (word forms)
        "אחד" to listOf("אֶחָד"),
        "שנים" to listOf("שְׁנַיִם"),
        "שלוש" to listOf("שָׁלוֹשׁ"),
        "ארבע" to listOf("אַרְבַּע"),
        "חמש" to listOf("חָמֵשׁ"),
        "שש" to listOf("שֵׁשׁ"),
        "שבע" to listOf("שֶׁבַע"),
        "שמונה" to listOf("שְׁמוֹנֶה"),
        "תשע" to listOf("תֵּשַׁע"),
        "עשר" to listOf("עֶשֶׂר"),
        "מאה" to listOf("מֵאָה"),
        "אלף" to listOf("אֶלֶף"),

        // Additional common words
        "אדם" to listOf("אָדָם"),
        "כל" to listOf("כֹּל"),
        "עם" to listOf("עַם", "עִם"),
        "כי" to listOf("כִּי"),
        "את" to listOf("אֶת", "אַתְּ"),
        "על" to listOf("עַל"),
        "מן" to listOf("מִן"),
        "אל" to listOf("אֶל", "אַל"),
        "הוא" to listOf("הוּא"),
        "היא" to listOf("הִיא"),
        "אני" to listOf("אֲנִי"),
        "אתה" to listOf("אַתָּה"),
        "הם" to listOf("הֵם"),
        "שם" to listOf("שָׁם", "שֵׁם"),
        "פה" to listOf("פֹּה"),
        "מה" to listOf("מָה"),
        "מי" to listOf("מִי"),
        "איפה" to listOf("אֵיפֹה"),
        "למה" to listOf("לָמָּה"),
        "מתי" to listOf("מָתַי"),
        "איך" to listOf("אֵיךְ")
    )

    fun suggest(word: String): List<String> {
        // Strip any existing nikud from input
        val stripped = stripNikud(word)
        return nikudMap[stripped] ?: emptyList()
    }

    fun hasNikud(word: String): Boolean {
        return nikudMap.containsKey(stripNikud(word))
    }

    private fun stripNikud(text: String): String {
        // Remove Unicode nikud range: 0x0591-0x05C7
        return text.filter { it.code !in 0x0591..0x05C7 }
    }
}
