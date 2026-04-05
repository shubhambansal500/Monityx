package com.bansalcoders.monityx.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bansalcoders.monityx.data.local.dao.CurrencyRateDao
import com.bansalcoders.monityx.data.local.dao.SubscriptionDao
import com.bansalcoders.monityx.data.local.entities.CurrencyRateEntity
import com.bansalcoders.monityx.data.local.entities.SubscriptionEntity

/**
 * Room database for the Subscription Manager app.
 *
 * Version history:
 *  1 → Initial schema
 *  2 → Added [SubscriptionEntity.sharedWith] column (DEFAULT 1)
 */
@Database(
    entities  = [SubscriptionEntity::class, CurrencyRateEntity::class],
    version   = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun currencyRateDao(): CurrencyRateDao

    companion object {
        const val DATABASE_NAME = "subscription_manager.db"

        /** Adds the sharedWith column with a default of 1 (solo subscription). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN sharedWith INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}
