package com.bansalcoders.monityx.domain.usecase

import com.bansalcoders.monityx.domain.model.BillingCycle
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Adds a new subscription after validating business rules.
 * Calculates [Subscription.nextBillingDate] automatically.
 */
class AddSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    data class Params(
        val subscription: Subscription,
    )

    sealed class Result {
        data class Success(val id: Long) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(params: Params): Result {
        val sub = params.subscription

        // Validate name
        if (sub.name.isBlank()) return Result.Error("Name cannot be empty")

        // Validate cost
        if (sub.cost <= 0.0) return Result.Error("Cost must be greater than 0")

        // Validate currency (basic ISO-4217 length check)
        if (sub.currency.length != 3) return Result.Error("Invalid currency code")

        // Ensure next billing date is in the future
        val nextBilling = calculateNextBillingDate(sub.startDate, sub.billingCycle)

        val validated = sub.copy(
            name = sub.name.trim(),
            nextBillingDate = nextBilling,
        )

        return try {
            val id = repository.addSubscription(validated)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    companion object {
        /**
         * Advances the start date by the billing cycle until it lands on a future date.
         */
        fun calculateNextBillingDate(startDate: LocalDate, cycle: BillingCycle): LocalDate {
            val today = LocalDate.now()
            var next = startDate
            while (!next.isAfter(today)) {
                next = when (cycle) {
                    BillingCycle.WEEKLY  -> next.plusWeeks(1)
                    BillingCycle.MONTHLY -> next.plusMonths(1)
                    BillingCycle.YEARLY  -> next.plusYears(1)
                }
            }
            return next
        }
    }
}
