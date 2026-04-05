package com.bansalcoders.monityx.domain.usecase

import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import javax.inject.Inject

/**
 * Updates an existing subscription, recalculating [nextBillingDate] as needed.
 */
class UpdateSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(subscription: Subscription): Result<Unit> {
        if (subscription.id == 0L) return Result.failure(IllegalArgumentException("Invalid subscription ID"))
        if (subscription.name.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        if (subscription.cost <= 0.0) return Result.failure(IllegalArgumentException("Cost must be > 0"))

        val nextBilling = AddSubscriptionUseCase.calculateNextBillingDate(
            subscription.startDate,
            subscription.billingCycle,
        )

        return try {
            repository.updateSubscription(subscription.copy(nextBillingDate = nextBilling))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
