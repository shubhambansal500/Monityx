package com.bansalcoders.monityx.data.repository

import com.bansalcoders.monityx.data.local.dao.SubscriptionDao
import com.bansalcoders.monityx.data.local.entities.SubscriptionEntity
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [SubscriptionRepository].
 * Delegates to Room [SubscriptionDao] and maps entities ↔ domain models.
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val dao: SubscriptionDao,
) : SubscriptionRepository {

    override fun getAllSubscriptions(): Flow<List<Subscription>> =
        dao.getAllSubscriptions().map { list -> list.map { it.toDomain() } }

    override fun getActiveSubscriptions(): Flow<List<Subscription>> =
        dao.getActiveSubscriptions().map { list -> list.map { it.toDomain() } }

    override suspend fun getSubscriptionById(id: Long): Subscription? =
        dao.getSubscriptionById(id)?.toDomain()

    override suspend fun addSubscription(subscription: Subscription): Long =
        dao.insertSubscription(SubscriptionEntity.fromDomain(subscription))

    override suspend fun updateSubscription(subscription: Subscription) =
        dao.updateSubscription(SubscriptionEntity.fromDomain(subscription))

    override suspend fun deleteSubscription(id: Long) =
        dao.deleteSubscriptionById(id)

    override suspend fun getUpcomingBillings(withinDays: Int): List<Subscription> {
        val today  = LocalDate.now()
        val maxDay = today.plusDays(withinDays.toLong())
        return dao.getUpcomingBillings(today.toEpochDay(), maxDay.toEpochDay())
            .map { it.toDomain() }
    }
}
