package com.bansalcoders.monityx.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import com.bansalcoders.monityx.utils.NotificationUtils
import com.bansalcoders.monityx.utils.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Runs once per day (scheduled by [ScheduleNotificationsUseCase]).
 *
 * Responsibilities:
 *  1. Post billing-reminder notifications for subscriptions renewing soon.
 *  2. Post a budget-warning notification when spend reaches 80 % of budget.
 *  3. Post a budget-exceeded notification when spend surpasses the budget.
 */
@HiltWorker
class BillingReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyRepository: CurrencyRepository,
    private val preferencesManager: PreferencesManager,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_REMINDER_DAYS    = "reminder_days"
        private const val DEFAULT_REMINDER_DAYS = 3
    }

    override suspend fun doWork(): Result {
        NotificationUtils.createNotificationChannel(context)

        val reminderDays  = inputData.getInt(KEY_REMINDER_DAYS, DEFAULT_REMINDER_DAYS)
        val baseCurrency  = preferencesManager.baseCurrency.first()
        val monthlyBudget = preferencesManager.monthlyBudget.first()

        // ── 1. Billing reminders ──────────────────────────────────────────────
        val upcoming = subscriptionRepository.getUpcomingBillings(withinDays = reminderDays)
        upcoming.forEach { subscription ->
            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), subscription.nextBillingDate)
            NotificationUtils.showBillingReminder(context, subscription, daysUntil)
        }

        // ── 2. Budget alerts ──────────────────────────────────────────────────
        if (monthlyBudget > 0.0) {
            val activeSubs = subscriptionRepository.getActiveSubscriptions().first()
            val totalMonthly = activeSubs.sumOf { sub ->
                val rate = runCatching {
                    currencyRepository.getExchangeRate(sub.currency, baseCurrency)
                }.getOrDefault(1.0)
                sub.monthlyCost * rate
            }

            val ratio = totalMonthly / monthlyBudget
            val percentageUsed = (ratio * 100).toInt()

            when {
                ratio >= 1.0 ->
                    NotificationUtils.showBudgetExceeded(
                        context      = context,
                        currentSpend = totalMonthly,
                        budget       = monthlyBudget,
                        currency     = baseCurrency,
                    )
                ratio >= 0.8 ->
                    NotificationUtils.showBudgetWarning(
                        context        = context,
                        currentSpend   = totalMonthly,
                        budget         = monthlyBudget,
                        currency       = baseCurrency,
                        percentageUsed = percentageUsed,
                    )
            }
        }

        return Result.success()
    }
}
