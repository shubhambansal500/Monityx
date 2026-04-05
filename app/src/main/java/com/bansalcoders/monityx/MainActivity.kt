package com.bansalcoders.monityx

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.bansalcoders.monityx.domain.usecase.ScheduleNotificationsUseCase
import com.bansalcoders.monityx.ui.navigation.SubscriptionManagerNavHost
import com.bansalcoders.monityx.ui.theme.SubscriptionManagerTheme
import com.bansalcoders.monityx.utils.NotificationUtils
import com.bansalcoders.monityx.utils.PreferencesManager
import com.bansalcoders.monityx.utils.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single Activity – hosts the full Jetpack Compose UI.
 * Theme is driven by user preference stored in DataStore.
 *
 * On every cold start:
 *  1. Notification channels are (re-)created.
 *  2. POST_NOTIFICATIONS runtime permission is requested on Android 13+.
 *  3. The periodic BillingReminderWorker is (re-)scheduled, respecting the
 *     user's "notifications enabled" and "reminder days" preferences.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var scheduleNotificationsUseCase: ScheduleNotificationsUseCase

    /**
     * Launcher for the POST_NOTIFICATIONS runtime permission (Android 13+).
     * After the user responds we immediately attempt to schedule the worker,
     * which is a no-op if notifications are disabled in Settings.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                lifecycleScope.launch { scheduleWorkerFromPrefs() }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── 1. Create notification channels (safe to call repeatedly). ────────
        NotificationUtils.createNotificationChannel(this)

        // ── 2. Schedule the daily reminder worker from stored preferences.
        //       This is idempotent: WorkManager uses ExistingPeriodicWorkPolicy.UPDATE
        //       so repeated calls just keep the worker alive / update its config. ──
        lifecycleScope.launch { scheduleWorkerFromPrefs() }

        // ── 3. Request POST_NOTIFICATIONS on Android 13+ if not yet granted. ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            SubscriptionManagerTheme(themeMode = themeMode) {
                SubscriptionManagerNavHost()
            }
        }
    }

    /** Reads persisted preferences and calls [ScheduleNotificationsUseCase.schedule]. */
    private suspend fun scheduleWorkerFromPrefs() {
        val enabled = preferencesManager.notificationsEnabled.first()
        val days    = preferencesManager.reminderDays.first()
        scheduleNotificationsUseCase.schedule(
            reminderDays         = days,
            notificationsEnabled = enabled,
        )
    }
}
