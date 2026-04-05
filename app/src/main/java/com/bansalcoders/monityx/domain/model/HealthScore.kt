package com.bansalcoders.monityx.domain.model

/**
 * Represents the overall financial health of a user's subscription portfolio.
 * Score is computed by [GetHealthScoreUseCase] using local heuristics — no server required.
 */
data class HealthScore(
    val score: Int,                       // 0–100
    val grade: HealthGrade,
    val breakdown: List<HealthDeduction>, // itemised deduction reasons
)

enum class HealthGrade(val label: String, val emoji: String) {
    EXCELLENT("Excellent", "🏆"),  // 85–100
    GOOD     ("Good",      "✅"),  // 70–84
    FAIR     ("Fair",      "⚠️"),  // 50–69
    POOR     ("Poor",      "🚨"),  // 30–49
    CRITICAL ("Critical",  "💀"); // 0–29

    companion object {
        fun fromScore(score: Int): HealthGrade = when {
            score >= 85 -> EXCELLENT
            score >= 70 -> GOOD
            score >= 50 -> FAIR
            score >= 30 -> POOR
            else        -> CRITICAL
        }
    }
}

/** A single reason why points were deducted from the health score. */
data class HealthDeduction(
    val reason: String,
    val points: Int,   // positive value = how many points were removed
)
