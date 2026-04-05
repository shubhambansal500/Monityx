package com.bansalcoders.monityx.domain.repository

import com.bansalcoders.monityx.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for subscription persistence.
 * Implementation lives in the data layer ([SubscriptionRepositoryImpl]).
 */
interface SubscriptionRepository {

    /** Reactive stream of all subscriptions. Emits on every change. */
    fun getAllSubscriptions(): Flow<List<Subscription>>

    /** Reactive stream of active-only subscriptions. */
    fun getActiveSubscriptions(): Flow<List<Subscription>>

    /** Returns a single subscription by ID, or null if not found. */
    suspend fun getSubscriptionById(id: Long): Subscription?

    /** Inserts a new subscription and returns the generated row ID. */
    suspend fun addSubscription(subscription: Subscription): Long

    /** Updates an existing subscription. */
    suspend fun updateSubscription(subscription: Subscription)

    /** Deletes a subscription by ID. */
    suspend fun deleteSubscription(id: Long)

    /** Returns subscriptions whose next billing date is within [withinDays] days. */
    suspend fun getUpcomingBillings(withinDays: Int = 7): List<Subscription>
}
