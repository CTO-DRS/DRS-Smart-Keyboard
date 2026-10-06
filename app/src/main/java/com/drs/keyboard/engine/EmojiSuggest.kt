package com.drs.keyboard.engine

/**
 * Rule-based emoji suggestions — a small static word→emoji table, fully
 * offline, no AI. While the user types (or right after a word is finished)
 * the IME asks this table for a matching emoji and offers it as a chip in
 * the suggestion strip. Supports English and Arabic keywords, plus prefix
 * rules for laughter ("ههه…", "haha…", "lo…").
 */
object EmojiSuggest {

    private val MAP: Map<String, String> = mapOf(
        // ---- English -------------------------------------------------------
        "thanks" to "🙏", "thank" to "🙏", "thx" to "🙏", "ty" to "🙏",
        "please" to "🙏", "sorry" to "😔",
        "love" to "❤️", "heart" to "❤️", "kiss" to "😘", "happy" to "😊",
        "smile" to "😊", "cool" to "😎", "wow" to "😮", "omg" to "😱",
        "sad" to "😢", "cry" to "😢", "crying" to "😢",
        "lol" to "😂", "lmao" to "😂", "rofl" to "😂",
        "yes" to "✅", "no" to "❌", "ok" to "👍", "okay" to "👍",
        "good" to "👍", "bad" to "👎", "hi" to "👋", "hello" to "👋",
        "bye" to "👋", "congrats" to "🎉", "congratulations" to "🎉",
        "birthday" to "🎂", "bday" to "🎂", "party" to "🎉",
        "coffee" to "☕", "tea" to "🍵", "food" to "🍽️", "eat" to "🍽️",
        "pizza" to "🍕", "apple" to "🍎", "cake" to "🍰",
        "sleep" to "😴", "tired" to "😴", "work" to "💼", "home" to "🏠",
        "car" to "🚗", "phone" to "📱", "fire" to "🔥", "star" to "⭐",
        "money" to "💰", "cash" to "💰", "time" to "⏰", "music" to "🎵",
        "song" to "🎵", "game" to "🎮", "play" to "🎮", "rain" to "🌧️",
        "sun" to "☀️", "moon" to "🌙", "night" to "🌙", "friend" to "🤝",
        "strong" to "💪", "rose" to "🌹", "flower" to "🌸", "gift" to "🎁",
        "book" to "📖", "read" to "📖", "idea" to "💡", "warning" to "⚠️",
        "wait" to "⏳", "photo" to "📷", "camera" to "📷", "email" to "📧",
        "lock" to "🔒", "key" to "🔑", "world" to "🌍", "rocket" to "🚀",
        // ---- Arabic --------------------------------------------------------
        "شكرا" to "🙏", "شكراً" to "🙏", "اشكرك" to "🙏", "لوسمحت" to "🙏",
        "آسف" to "😔", "اسف" to "😔", "عفوا" to "😔",
        "حب" to "❤️", "أحبك" to "❤️", "احبك" to "❤️", "قلب" to "❤️",
        "قبلة" to "😘", "سعيد" to "😊", "فرح" to "😊", "ابتسامة" to "😊",
        "جميل" to "😍", "رائع" to "😍", "واو" to "😮", "مذهل" to "😱",
        "حزين" to "😢", "بكاء" to "😢", "ابكي" to "😢", "دموع" to "😢",
        "مبروك" to "🎉", "مبارك" to "🎉", "تهانينا" to "🎉", "عيد" to "🎂",
        "ميلاد" to "🎂", "حفلة" to "🎉",
        "قهوة" to "☕", "شاي" to "🍵", "طعام" to "🍽️", "أكل" to "🍽️",
        "اكل" to "🍽️", "بيتزا" to "🍕", "تفاح" to "🍎", "كيك" to "🍰",
        "نوم" to "😴", "نعسان" to "😴", "تعبان" to "😴", "عمل" to "💼",
        "دوام" to "💼", "بيت" to "🏠", "منزل" to "🏠", "سيارة" to "🚗",
        "هاتف" to "📱", "جوال" to "📱", "نار" to "🔥", "نجم" to "⭐",
        "نجمة" to "⭐", "مال" to "💰", "فلوس" to "💰", "وقت" to "⏰",
        "ساعة" to "⏰", "موسيقى" to "🎵", "أغنية" to "🎵", "اغنية" to "🎵",
        "لعبة" to "🎮", "مطر" to "🌧️", "شمس" to "☀️", "قمر" to "🌙",
        "ليل" to "🌙", "صديق" to "🤝", "صداقة" to "🤝", "قوة" to "💪",
        "وردة" to "🌹", "ورد" to "🌹", "زهرة" to "🌸", "هدية" to "🎁",
        "كتاب" to "📖", "قراءة" to "📖", "فكرة" to "💡", "تحذير" to "⚠️",
        "انتظار" to "⏳", "صورة" to "📷", "بريد" to "📧", "قفل" to "🔒",
        "مفتاح" to "🔑", "عالم" to "🌍", "صاروخ" to "🚀"
    )

    /** Laughter prefixes: any word starting with these maps to 😂. */
    private val PREFIX_RULES: List<Pair<String, String>> = listOf(
        "هه" to "😂",      // هه، ههه، ههههه…
        "haha" to "😂", "hehe" to "😂", "hihi" to "😂",
        "looool" to "😂", "loool" to "😂"
    )

    /** Returns the emoji for a word, or null. `w` may carry any casing. */
    fun forWord(w: String): String? {
        val word = w.trim().lowercase()
        if (word.isEmpty() || word.length > 18) return null
        MAP[word]?.let { return it }
        for ((prefix, emoji) in PREFIX_RULES) {
            if (word.startsWith(prefix)) return emoji
        }
        return null
    }

    /** True when the given suggestion text is one of our emoji chips. */
    fun isEmojiChip(s: String): Boolean = MAP.containsValue(s.trim())
}
