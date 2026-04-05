package com.bansalcoders.monityx.domain.usecase

import com.bansalcoders.monityx.domain.model.*
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Analyses the subscription portfolio and returns a prioritised list of
 * [SavingsSuggestion] items, each with concrete monthly and yearly savings.
 *
 * Sources of savings (in priority order):
 *  1. Inactive subscriptions        → full monthly cost saved by canceling
 *  2. Duplicate-category services   → save the most expensive duplicate(s)
 *  3. Monthly → yearly billing      → ~15 % discount on qualifying subs
 *  4. Likely-unused yearly subs     → started > 6 months ago, never renewed
 *
 * Each subscription appears at most once in the result list.
 */
class GetSavingsSuggestionsUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyRepository: CurrencyRepository,
) {
    suspend operator fun invoke(baseCurrency: String): List<SavingsSuggestion> {
        val allSubs    = subscriptionRepository.getAllSubscriptions().first()
        val activeSubs = allSubs.filter { it.isActive }

        val suggestions  = mutableListOf<SavingsSuggestion>()
        val handledIds   = mutableSetOf<Long>()

        // ── Pre-fetch all rates once (suspend calls live here) ────────────────
        val rateCache: Map<String, Double> = allSubs
            .map { it.currency }
            .distinct()
            .associateWith { currency ->
                try { currencyRepository.getExchangeRate(currency, baseCurrency) }
                catch (_: Exception) { 1.0 }
            }

        fun rate(currency: String): Double = rateCache[currency] ?: 1.0

        // ── 1. Inactive subscriptions ─────────────────────────────────────────
        allSubs.filter { !it.isActive }.forEach { sub ->
            val monthly = sub.monthlyCost * rate(sub.currency)
            suggestions += SavingsSuggestion(
                subscription  = sub,
                reason        = SuggestionReason.INACTIVE,
                monthlySavings = monthly,
                yearlySavings  = monthly * 12.0,
            )
            handledIds += sub.id
        }

        // ── 2. Duplicate categories (keep cheapest, suggest cancelling rest) ──
        activeSubs
            .groupBy { it.category }
            .filter { (_, list) -> list.size >= 2 }
            .forEach { (_, list) ->
                // Sort most → least expensive in base currency
                val sorted = list.sortedByDescending { sub ->
                    sub.monthlyCost * rate(sub.currency)
                }
                // Suggest cancelling all but the cheapest alternative
                sorted.dropLast(1).forEach { sub ->
                    if (sub.id !in handledIds) {
                        val monthly = sub.monthlyCost * rate(sub.currency)
                        suggestions += SavingsSuggestion(
                            subscription  = sub,
                            reason        = SuggestionReason.DUPLICATE_CATEGORY,
                            monthlySavings = monthly,
                            yearlySavings  = monthly * 12.0,
                        )
                        handledIds += sub.id
                    }
                }
            }

        // ── 3. Monthly-billed → switch to yearly (~15 % saving) ──────────────
        activeSubs
            .filter { it.billingCycle == BillingCycle.MONTHLY }
            .filter { sub -> sub.monthlyCost * rate(sub.currency) >= 3.0 } // meaningful threshold
            .forEach { sub ->
                if (sub.id !in handledIds) {
                    val yearlySaving = sub.monthlyCost * rate(sub.currency) * 12.0 * 0.15
                    suggestions += SavingsSuggestion(
                        subscription  = sub,
                        reason        = SuggestionReason.SWITCH_TO_YEARLY,
                        monthlySavings = yearlySaving / 12.0,
                        yearlySavings  = yearlySaving,
                    )
                    handledIds += sub.id
                }
            }

        // ── 4. Possibly-unused yearly subscriptions ───────────────────────────
        val sixMonthsAgo = LocalDate.now().minusMonths(6)
        activeSubs
            .filter { it.billingCycle == BillingCycle.YEARLY && it.startDate.isBefore(sixMonthsAgo) }
            .forEach { sub ->
                if (sub.id !in handledIds) {
                    val monthly = sub.monthlyCost * rate(sub.currency)
                    suggestions += SavingsSuggestion(
                        subscription  = sub,
                        reason        = SuggestionReason.LIKELY_UNUSED,
                        monthlySavings = monthly,
                        yearlySavings  = monthly * 12.0,
                    )
                    handledIds += sub.id
                }
            }

        // Sort by highest yearly savings first
        return suggestions.sortedByDescending { it.yearlySavings }
    }
}
