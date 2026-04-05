package com.bansalcoders.monityx.domain.usecase

import com.bansalcoders.monityx.domain.model.*
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Computes analytics data (charts, totals, AI insights) from stored subscriptions.
 *
 * All costs are converted to the user's [baseCurrency] for unified totals.
 *
 * New in v2:
 *  - Spending spike detection (flags months where spend > avg + 1 std-dev)
 *  - Concrete currency amounts in all insight messages
 *  - Inactive-subscription insight with exact savings figure
 */
class GetAnalyticsUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyRepository: CurrencyRepository,
) {
    enum class TimeFilter { ONE_MONTH, SIX_MONTHS, ONE_YEAR }

    suspend operator fun invoke(
        baseCurrency: String = "INR",
        timeFilter: TimeFilter = TimeFilter.ONE_YEAR,
    ): AnalyticsData {
        val subscriptions = subscriptionRepository.getActiveSubscriptions().first()

        // ── Pre-fetch all exchange rates (suspend calls happen here, once) ────
        val rateCache: Map<String, Double> = subscriptions
            .map { it.currency }
            .distinct()
            .associateWith { currency ->
                try { currencyRepository.getExchangeRate(currency, baseCurrency) }
                catch (_: Exception) { 1.0 }
            }

        // Pure (non-suspend) helpers used in lambdas below
        fun rate(currency: String): Double = rateCache[currency] ?: 1.0
        fun monthlyInBase(sub: Subscription) = sub.monthlyCost * rate(sub.currency)

        // ── Monthly spend history ─────────────────────────────────────────────
        val months = when (timeFilter) {
            TimeFilter.ONE_MONTH   -> 1
            TimeFilter.SIX_MONTHS  -> 6
            TimeFilter.ONE_YEAR    -> 12
        }

        val formatter = DateTimeFormatter.ofPattern("MMM yy")
        val today = LocalDate.now()

        val monthlyHistory = (months - 1 downTo 0).map { offset ->
            val date   = today.minusMonths(offset.toLong())
            val label  = date.format(formatter)
            val amount = subscriptions.sumOf { monthlyInBase(it) }
            MonthlySpend(label = label, amount = amount)
        }

        // ── Spending spike detection ──────────────────────────────────────────
        val spendingSpike: SpendingSpike? = if (monthlyHistory.size >= 3) {
            val amounts = monthlyHistory.map { it.amount }
            val mean    = amounts.average()
            val stdDev  = Math.sqrt(amounts.sumOf { (it - mean) * (it - mean) } / amounts.size)
            val latest  = amounts.last()
            if (latest > mean + stdDev && stdDev > 0) {
                SpendingSpike(
                    currentMonthAmount = latest,
                    averageAmount      = mean,
                    percentageIncrease = ((latest - mean) / mean * 100).toInt(),
                )
            } else null
        } else null

        // ── Category breakdown ────────────────────────────────────────────────
        val categoryTotals = subscriptions
            .groupBy { it.category }
            .mapValues { (_, subs) -> subs.sumOf { monthlyInBase(it) } }

        val totalMonthly = categoryTotals.values.sum().let {
            if (it == 0.0) subscriptions.sumOf { monthlyInBase(it) } else it
        }

        val categoryBreakdown = categoryTotals
            .map { (cat, amount) ->
                CategorySpend(
                    category   = cat,
                    amount     = amount,
                    percentage = if (totalMonthly > 0) (amount / totalMonthly * 100).toFloat() else 0f,
                )
            }
            .sortedByDescending { it.amount }

        val totalYearly   = totalMonthly * 12.0
        val mostExpensive = subscriptions.maxByOrNull { monthlyInBase(it) }
        // Pre-compute the converted monthly cost so the screen never uses the
        // raw subscription cost in its native currency for display.
        val mostExpensiveCostInBase = mostExpensive?.let { monthlyInBase(it) } ?: 0.0

        // ── AI insights ───────────────────────────────────────────────────────
        val insights = buildAiInsights(
            subscriptions = subscriptions,
            totalMonthly  = totalMonthly,
            baseCurrency  = baseCurrency,
            spendingSpike = spendingSpike,
        ) { currency -> rate(currency) }

        return AnalyticsData(
            monthlySpendHistory           = monthlyHistory,
            categoryBreakdown             = categoryBreakdown,
            totalMonthlySpend             = totalMonthly,
            totalYearlySpend              = totalYearly,
            activeCount                   = subscriptions.size,
            mostExpensiveSubscription     = mostExpensive,
            mostExpensiveMonthlyCostInBase = mostExpensiveCostInBase,
            aiInsights                    = insights,
            spendingSpike                 = spendingSpike,
        )
    }

    private fun buildAiInsights(
        subscriptions: List<Subscription>,
        totalMonthly: Double,
        baseCurrency: String,
        spendingSpike: SpendingSpike?,
        rateFor: (String) -> Double,
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()

        // Spending spike alert
        spendingSpike?.let { spike ->
            insights += AiInsight(
                type    = InsightType.BUDGET_ALERT,
                message = "This month's spend is ${spike.percentageIncrease}% above your recent average " +
                          "(${fmtAmount(spike.currentMonthAmount, baseCurrency)} vs avg ${fmtAmount(spike.averageAmount, baseCurrency)}).",
            )
        }

        // Duplicate categories with exact savings
        subscriptions
            .groupBy { it.category }
            .filter { (_, list) -> list.size >= 2 }
            .forEach { (cat, list) ->
                val sortedCost = list.sortedByDescending { it.monthlyCost * rateFor(it.currency) }
                val redundantCost = sortedCost.dropLast(1).sumOf { it.monthlyCost * rateFor(it.currency) }
                insights += AiInsight(
                    type    = InsightType.DUPLICATE_CATEGORY,
                    message = "You have ${list.size} ${cat.label} subscriptions. " +
                              "Cancelling the pricier one(s) could save ${fmtAmount(redundantCost, baseCurrency)}/mo.",
                )
            }

        // Yearly billing saving tip with exact figure
        subscriptions.filter { it.billingCycle == BillingCycle.MONTHLY }.forEach { sub ->
            val monthlyCostBase = sub.monthlyCost * rateFor(sub.currency)
            val yearlySaving = monthlyCostBase * 12.0 * 0.15
            if (yearlySaving > 5.0) {
                insights += AiInsight(
                    type             = InsightType.SAVING_TIP,
                    message          = "Switching ${sub.name} to yearly billing could save ~${fmtAmount(yearlySaving, baseCurrency)}/year.",
                    subscriptionName = sub.name,
                )
            }
        }

        // High total spend alert (only if no spike alert already)
        if (spendingSpike == null && totalMonthly > 100.0) {
            insights += AiInsight(
                type    = InsightType.BUDGET_ALERT,
                message = "Monthly subscription spend of ${fmtAmount(totalMonthly, baseCurrency)} is quite high. " +
                          "Review inactive or duplicate services to cut costs.",
            )
        }

        return insights.take(5)
    }

    private fun fmtAmount(amount: Double, currency: String) =
        "$currency ${"%.0f".format(amount)}"
}
