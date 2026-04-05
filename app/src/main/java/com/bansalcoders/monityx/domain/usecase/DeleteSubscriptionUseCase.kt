package com.bansalcoders.monityx.domain.usecase

import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import javax.inject.Inject

/** Deletes a subscription by ID. */
class DeleteSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(id: Long): Result<Unit> = try {
        repository.deleteSubscription(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
