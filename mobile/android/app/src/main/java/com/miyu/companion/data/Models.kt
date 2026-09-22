package com.miyu.companion.data

enum class AgeProfile(val displayAr: String, val displayEn: String, val minAge: Int, val maxAge: Int) {
    CHILD_3_5("٣-٥ سنوات · لعب آمن", "3-5 years · gentle play", 3, 5),
    CHILD_6_8("٦-٨ سنوات · لعب آمن", "6-8 years · gentle play", 6, 8),
    CHILD_9_12("٩-١٢ سنة · لعب آمن", "9-12 years · safe play", 9, 12),
    TEEN_13_17("١٣-١٧ سنة · وضع آمن", "13-17 years · safe mode", 13, 17),
    ADULT_18_PLUS("١٨+ · وضع آمن", "18+ · safe mode", 18, 99);

    companion object {
        fun fromAge(age: Int): AgeProfile = when {
            age <= 5 -> CHILD_3_5
            age <= 8 -> CHILD_6_8
            age <= 12 -> CHILD_9_12
            age <= 17 -> TEEN_13_17
            else -> ADULT_18_PLUS
        }
    }
}

enum class ToyType(val key: String, val labelEn: String, val labelAr: String, val emoji: String, val keywords: List<String>) {
    CAR("car", "Toy car", "عربية لعبة", "🚗", listOf("car", "toy car", "سيارة", "عربية", "عربيه", "سياره")),
    BALL("ball", "Ball", "كرة", "⚽", listOf("ball", "كرة", "كوره")),
    TEDDY("teddy", "Teddy bear", "دبدوب", "🧸", listOf("teddy", "bear", "دبدوب", "دب")),
    BOOK("book", "Story book", "كتاب", "📚", listOf("book", "story", "كتاب", "قصه", "قصة")),
    ROCKET("rocket", "Rocket", "صاروخ", "🚀", listOf("rocket", "صاروخ")),
    FLOWER("flower", "Flower", "وردة", "🌸", listOf("flower", "وردة", "زهرة", "ورد")),
    PUZZLE("puzzle", "Puzzle", "بازل", "🧩", listOf("puzzle", "بازل", "لغز")),
    BALLOON("balloon", "Balloon", "بالونة", "🎈", listOf("balloon", "بالونة", "بلونه")),
    MUSICAL("musical", "Musical toy", "لعبة موسيقية", "🎵", listOf("music", "musical", "موسيقى", "مزيكا"));

    companion object {
        fun fromInput(input: String): ToyType? {
            val lower = input.lowercase()
            return values().firstOrNull { toy -> toy.keywords.any { lower.contains(it) } }
        }
    }
}

data class FloatingPosition(
    val x: Float = 0.85f,
    val y: Float = 0.3f,
    val size: Float = 1f, // 0.5 to 1.5
    val alpha: Float = 1f,
    val snapToEdge: Boolean = true,
    val isMini: Boolean = false,
    val isHidden: Boolean = false,
    val clickThrough: Boolean = false,
    // "Stop motion": freezes breathing / blinking / toy animation for calm play
    // and for users who prefer reduced motion.
    val motionPaused: Boolean = false
)

data class MiyuState(
    val age: Int? = null,
    val language: String = "ar",
    val toy: String? = null,
    val respectMeter: Int = 100,
    val mood: String = "cozy",
    val guardianConfirmed: Boolean = false,
    val overlayEnabled: Boolean = false,
    val focusMinutes: Int = 25,
    val memories: List<String> = emptyList()
)

data class OwnerConfig(
    val isOwnerBuild: Boolean = false,
    val buildId: String = "",
    val version: String = "1.2.5"
)
