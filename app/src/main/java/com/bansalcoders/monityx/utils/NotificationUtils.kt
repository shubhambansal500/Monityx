package com.bansalcoders.monityx.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bansalcoders.monityx.R
import com.bansalcoders.monityx.domain.model.Subscription

/**
 * Creates and displays notifications for billing reminders and budget alerts.
 *
 * POST_NOTIFICATIONS permission is checked by the caller (WorkManager worker).
 * No sensitive user data is transmitted – all data stays on-device.
 */
object NotificationUtils {

    const val CHANNEL_ID          = "billing_reminders"
    const val CHANNEL_NAME        = "Billing Reminders"

    private const val BUDGET_CHANNEL_ID   = "budget_alerts"
    private const val BUDGET_CHANNEL_NAME = "Budget Alerts"

    // Stable notification IDs for budget alerts (don't collide with subscription IDs)
    private const val NOTIF_BUDGET_WARNING  = 900_001
    private const val NOTIF_BUDGET_EXCEEDED = 900_002

    // ── Channel setup ─────────────────────────────────────────────────────────

    /** Creates all notification channels (safe to call repeatedly). */
    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Reminds you about upcoming subscription renewals"
                    enableVibration(true)
                }
            )
        }

        if (manager.getNotificationChannel(BUDGET_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(BUDGET_CHANNEL_ID, BUDGET_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alerts when you approach or exceed your monthly subscription budget"
                    enableVibration(true)
                }
            )
        }
    }

    // ── Billing reminder ──────────────────────────────────────────────────────

    /**
     * Shows a billing reminder notification for [subscription].
     * Silently no-ops if POST_NOTIFICATIONS permission is not granted.
     */
    fun showBillingReminder(
        context: Context,
        subscription: Subscription,
        daysUntilRenewal: Long,
    ) {
        if (!hasNotificationPermission(context)) return

        val amount   = CurrencyUtils.formatAmount(subscription.cost, subscription.currency)
        val daysText = when (daysUntilRenewal) {
            0L   -> "today"
            1L   -> "tomorrow"
            else -> "in $daysUntilRenewal days"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${subscription.name} renews $daysText")
            .setContentText("$amount will be charged on ${DateUtils.format(subscription.nextBillingDate)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your ${subscription.name} subscription ($amount) renews $daysText.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(subscription.id.toInt(), notification)
    }

    // ── Budget alerts ─────────────────────────────────────────────────────────

    /**
     * Posts a warning when the user is approaching their budget (80–99 %).
     */
    fun showBudgetWarning(
        context: Context,
        currentSpend: Double,
        budget: Double,
        currency: String,
        percentageUsed: Int,
    ) {
        if (!hasNotificationPermission(context)) return

        val spendFmt  = CurrencyUtils.formatAmount(currentSpend, currency)
        val budgetFmt = CurrencyUtils.formatAmount(budget, currency)

        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Subscription budget at $percentageUsed%")
            .setContentText("You've spent $spendFmt of your $budgetFmt monthly budget.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "You've used $percentageUsed% of your monthly subscription budget.\n" +
                        "Spent: $spendFmt  |  Budget: $budgetFmt"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_BUDGET_WARNING, notification)
    }

    /**
     * Posts an alert when the monthly budget has been exceeded.
     */
    fun showBudgetExceeded(
        context: Context,
        currentSpend: Double,
        budget: Double,
        currency: String,
    ) {
        if (!hasNotificationPermission(context)) return

        val spendFmt    = CurrencyUtils.formatAmount(currentSpend, currency)
        val budgetFmt   = CurrencyUtils.formatAmount(budget, currency)
        val overBy      = CurrencyUtils.formatAmount(currentSpend - budget, currency)

        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Monthly subscription budget exceeded!")
            .setContentText("You're $overBy over your $budgetFmt budget.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Your subscriptions total $spendFmt — $overBy over your $budgetFmt monthly budget. " +
                        "Open Monityx to review your spending."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_BUDGET_EXCEEDED, notification)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
