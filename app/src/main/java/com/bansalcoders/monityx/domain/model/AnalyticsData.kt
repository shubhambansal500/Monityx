package com.bansalcoders.monityx.domain.model

/**
 * Aggregated analytics models consumed by the Analytics screen.
 */

/** Monthly spend data point for a line chart. */
data class MonthlySpend(
    val label: String,   // e.g. "Jan 24"
    val amount: Double,  // in base currency
)

/** Per-category spend data for a pie / donut chart. */
data class CategorySpend(
    val category: Category,
    val amount: Double,
    val percentage: Float,
)

/** Complete analytics data returned by [GetAnalyticsUseCase]. */
data class AnalyticsData(
    val monthlySpendHistory: List<MonthlySpend>,
    val categoryBreakdown: List<CategorySpend>,
    val totalMonthlySpend: Double,
    val totalYearlySpend: Double,
    val activeCount: Int,
    val mostExpensiveSubscription: Subscription?,
    /**
     * The most-expensive subscription's monthly cost already converted to the
     * user's base currency. Always use this for display — it updates when the
     * base currency changes, unlike [mostExpensiveSubscription.cost].
     */
    val mostExpensiveMonthlyCostInBase: Double = 0.0,
    val aiInsights: List<AiInsight>,
    /** Non-null when the latest month shows an unusual spending spike. */
    val spendingSpike: SpendingSpike? = null,
)

/**
 * Detected when the current month's spend is more than one standard deviation
 * above the recent monthly average.
 */
data class SpendingSpike(
    val currentMonthAmount: Double,
    val averageAmount: Double,
    val percentageIncrease: Int,
)

/** Simple AI insight generated client-side (no server call required). */
data class AiInsight(
    val type: InsightType,
    val message: String,
    val subscriptionName: String? = null,
)

enum class InsightType {
    UNUSED_LIKELY,       // Subscription that hasn't been used recently (heuristic)
    DUPLICATE_CATEGORY,  // Two or more subs in the same expensive category
    BUDGET_ALERT,        // Monthly spend exceeds a threshold
    SAVING_TIP,          // Switch to yearly billing to save
}
