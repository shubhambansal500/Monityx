package com.bansalcoders.monityx

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bansalcoders.monityx.utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * - Initialises Hilt dependency injection.
 * - Provides Hilt-aware [WorkerFactory] to WorkManager (manual init).
 * - Creates the billing reminder notification channel on startup.
 */
@HiltAndroidApp
class MonityxApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationUtils.createNotificationChannel(this)
    }
}
