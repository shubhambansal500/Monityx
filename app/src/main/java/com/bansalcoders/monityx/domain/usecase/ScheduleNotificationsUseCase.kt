package com.bansalcoders.monityx.domain.usecase

import android.content.Context
import androidx.work.*
import com.bansalcoders.monityx.workers.BillingReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Schedules or cancels the periodic [BillingReminderWorker] via WorkManager.
 * The worker runs daily and fires a notification for subscriptions due within [reminderDays].
 */
class ScheduleNotificationsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val WORK_NAME = "billing_reminder_periodic"
    }

    /** Enqueues the reminder worker (idempotent – safe to call multiple times). */
    fun schedule(reminderDays: Int, notificationsEnabled: Boolean) {
        val workManager = WorkManager.getInstance(context)

        if (!notificationsEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val data = workDataOf(
            BillingReminderWorker.KEY_REMINDER_DAYS to reminderDays,
        )

        val request = PeriodicWorkRequestBuilder<BillingReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Cancels the scheduled reminder worker. */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
