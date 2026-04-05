package com.bansalcoders.monityx.domain.model

/**
 * Predefined subscription categories.
 * Users can also choose [OTHER] for custom subscriptions.
 */
enum class Category(val label: String, val emoji: String) {
    ENTERTAINMENT("Entertainment", "🎬"),
    MUSIC("Music",           "🎵"),
    VIDEO("Video Streaming", "📺"),
    GAMING("Gaming",         "🎮"),
    PRODUCTIVITY("Productivity", "💼"),
    CLOUD_STORAGE("Cloud Storage", "☁️"),
    NEWS("News & Media",     "📰"),
    FITNESS("Health & Fitness", "💪"),
    EDUCATION("Education",   "📚"),
    FOOD("Food & Delivery",  "🍔"),
    SHOPPING("Shopping",     "🛒"),
    FINANCE("Finance",       "💳"),
    UTILITIES("Utilities",   "🔧"),
    OTHER("Other",           "📦");

    companion object {
        fun fromLabel(label: String): Category =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: OTHER
    }
}
