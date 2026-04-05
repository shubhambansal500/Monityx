package com.bansalcoders.monityx.domain.usecase

import com.bansalcoders.monityx.domain.model.*
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import com.bansalcoders.monityx.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Computes a 0–100 Subscription Health Score for the user's portfolio.
 *
 * Algorithm (starts at 100, deductions applied):
 *  - Each inactive subscription:          –8 pts  (cap –24)
 *  - Duplicate services (same category):  –5 pts  (cap –20)
 *  - Budget exceeded:                     –20 pts
 *  - Budget 80–99 % used:                 –10 pts
 *  - Yearly subs inactive > 6 months:     –6 pts  (cap –18)
 */
class GetHealthScoreUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyRepository: CurrencyRepository,
    private val preferencesManager: PreferencesManager,
) {
    suspend operator fun invoke(baseCurrency: String): HealthScore {
        val allSubs    = subscriptionRepository.getAllSubscriptions().first()
        val activeSubs = allSubs.filter { it.isActive }

        val deductions = mutableListOf<HealthDeduction>()
        var score = 100

        // ── 1. Inactive subscriptions ─────────────────────────────────────────
        val inactiveCount = allSubs.count { !it.isActive }
        if (inactiveCount > 0) {
            val penalty = minOf(inactiveCount * 8, 24)
            deductions += HealthDeduction(
                reason = "$inactiveCount inactive subscription${if (inactiveCount > 1) "s" else ""} still tracked",
                points = penalty,
            )
            score -= penalty
        }

        // ── 2. Duplicate categories ───────────────────────────────────────────
        val extraDups = activeSubs
            .groupBy { it.category }
            .values
            .sumOf { list -> maxOf(0, list.size - 1) }
        if (extraDups > 0) {
            val penalty = minOf(extraDups * 5, 20)
            deductions += HealthDeduction(
                reason = "Overlapping services in $extraDups category slot${if (extraDups > 1) "s" else ""}",
                points = penalty,
            )
            score -= penalty
        }

        // ── 3. Budget status ──────────────────────────────────────────────────
        val rawBudget      = preferencesManager.monthlyBudget.first()
        val budgetCurrency = preferencesManager.budgetCurrency.first()
        if (rawBudget > 0.0) {
            val totalMonthly = activeSubs.sumOf { sub ->
                val rate = runCatching {
                    currencyRepository.getExchangeRate(sub.currency, baseCurrency)
                }.getOrDefault(1.0)
                sub.monthlyCost * rate
            }
            // Convert the stored budget to base currency before comparing
            val budgetInBase = if (budgetCurrency == baseCurrency) {
                rawBudget
            } else {
                runCatching {
                    currencyRepository.getExchangeRate(budgetCurrency, baseCurrency) * rawBudget
                }.getOrDefault(rawBudget)
            }
            val ratio = totalMonthly / budgetInBase
            when {
                ratio >= 1.0 -> {
                    deductions += HealthDeduction("Monthly budget exceeded", 20)
                    score -= 20
                }
                ratio >= 0.8 -> {
                    val pct = (ratio * 100).toInt()
                    deductions += HealthDeduction("Spending at $pct% of monthly budget", 10)
                    score -= 10
                }
            }
        }

        // ── 4. Possibly unused yearly subscriptions ───────────────────────────
        val sixMonthsAgo = LocalDate.now().minusMonths(6)
        val unusedYearly = activeSubs.count {
            it.billingCycle == BillingCycle.YEARLY && it.startDate.isBefore(sixMonthsAgo)
        }
        if (unusedYearly > 0) {
            val penalty = minOf(unusedYearly * 6, 18)
            deductions += HealthDeduction(
                reason = "$unusedYearly yearly subscription${if (unusedYearly > 1) "s" else ""} may be underused",
                points = penalty,
            )
            score -= penalty
        }

        val finalScore = score.coerceIn(0, 100)
        return HealthScore(
            score     = finalScore,
            grade     = HealthGrade.fromScore(finalScore),
            breakdown = deductions,
        )
    }
}
