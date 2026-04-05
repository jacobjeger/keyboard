package com.megalife.ime.feature

/**
 * Filters emoji to remove inappropriate content for kosher phone usage.
 * Blocks: alcohol, suggestive/romantic, violent, gambling, magic/occult.
 */
object KosherEmojiFilter {

    /** Emoji that should be blocked on kosher phones */
    private val blockedEmoji = setOf(
        // Alcohol
        "\uD83C\uDF7A", // beer mug
        "\uD83C\uDF7B", // clinking beer mugs
        "\uD83C\uDF77", // wine glass
        "\uD83E\uDD42", // clinking glasses
        "\uD83C\uDF78", // cocktail glass
        "\uD83C\uDF79", // tropical drink
        "\uD83E\uDD43", // tumbler glass
        "\uD83C\uDF76", // sake
        "\uD83E\uDDC9", // mate
        "\uD83C\uDF7E", // champagne

        // Suggestive / Romantic
        "\uD83D\uDC8B", // kiss mark
        "\uD83D\uDC44", // mouth
        "\uD83D\uDC45", // tongue
        "\uD83D\uDC84", // lipstick
        "\uD83D\uDC85", // nail polish
        "\uD83D\uDC59", // bikini
        "\uD83D\uDC60", // high heel shoe
        "\uD83D\uDC61", // woman's sandal
        "\uD83E\uDE72", // briefs
        "\uD83D\uDEBB", // restroom

        // Violence / Weapons
        "\uD83D\uDD2B", // pistol
        "\uD83D\uDCA3", // bomb
        "\uD83D\uDD2A", // knife
        "\uD83D\uDDE1", // dagger
        "\u2694\uFE0F",  // crossed swords
        "\uD83C\uDFF9", // bow and arrow
        "\uD83D\uDD2E", // crystal ball
        "\uD83E\uDE84", // magic wand

        // Gambling
        "\uD83C\uDFB0", // slot machine
        "\uD83C\uDCCF", // joker card
        "\uD83C\uDFB2", // dice

        // Occult / Magic
        "\uD83E\uDDD9", // mage
        "\uD83E\uDDDB", // vampire
        "\uD83E\uDDDD", // elf
        "\uD83E\uDDDE", // genie
        "\uD83E\uDDDF", // zombie
        "\uD83D\uDC79", // ogre
        "\uD83D\uDC7A", // goblin
        "\uD83D\uDC7B", // ghost
        "\uD83D\uDC7D", // alien
        "\uD83D\uDC7E", // alien monster
        "\uD83D\uDC80", // skull
        "\u2620\uFE0F",  // skull and crossbones
        "\uD83E\uDDB4", // bone
        "\uD83C\uDF83", // jack-o-lantern
        "\uD83D\uDD73\uFE0F", // hole

        // Miscellaneous inappropriate
        "\uD83D\uDCA9", // pile of poo
        "\uD83D\uDD95", // middle finger
        "\uD83E\uDD2C", // face with symbols on mouth (swearing)
    )

    /** Blocked emoji name keywords */
    private val blockedKeywords = setOf(
        "beer", "wine", "cocktail", "alcohol", "drunk",
        "kiss", "bikini", "lingerie",
        "gun", "pistol", "bomb", "knife", "sword", "weapon",
        "skull", "skeleton", "zombie", "vampire", "ghost",
        "devil", "demon", "witch", "wizard", "magic",
        "gambling", "casino", "slot",
        "cigarette", "smoking", "drug"
    )

    /** Check if an emoji should be filtered out */
    fun isBlocked(emoji: String): Boolean {
        return emoji in blockedEmoji
    }

    /** Check if an emoji name matches blocked keywords */
    fun isNameBlocked(name: String): Boolean {
        val lower = name.lowercase()
        return blockedKeywords.any { lower.contains(it) }
    }

    /** Filter a list of emojis, removing blocked ones */
    fun filter(emojis: List<EmojiData.Emoji>): List<EmojiData.Emoji> {
        return emojis.filter { !isBlocked(it.emoji) && !isNameBlocked(it.name) }
    }
}
