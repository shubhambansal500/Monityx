package com.bansalcoders.monityx.data.local.dao

import androidx.room.*
import com.bansalcoders.monityx.data.local.entities.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO for subscription CRUD and queries. */
@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextBillingDate ASC")
    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionById(id: Long): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(entity: SubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(entity: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Long)

    /**
     * Returns subscriptions where nextBillingDate is between today and [maxEpochDay].
     * Used by [BillingReminderWorker] to find upcoming renewals.
     */
    @Query(
        """
        SELECT * FROM subscriptions
        WHERE isActive = 1
          AND nextBillingDate >= :todayEpochDay
          AND nextBillingDate <= :maxEpochDay
        ORDER BY nextBillingDate ASC
        """
    )
    suspend fun getUpcomingBillings(todayEpochDay: Long, maxEpochDay: Long): List<SubscriptionEntity>

    @Query("SELECT COUNT(*) FROM subscriptions WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>
}
