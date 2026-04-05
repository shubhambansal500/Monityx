package com.bansalcoders.monityx.domain.usecase

import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Returns a reactive stream of subscriptions with optional search and sort. */
class GetSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    enum class SortOrder { NAME, COST_HIGH, COST_LOW, NEXT_BILLING, CATEGORY }

    data class Params(
        val query: String = "",
        val sortOrder: SortOrder = SortOrder.NEXT_BILLING,
        val filterCategory: String? = null,  // null = show all
        val activeOnly: Boolean = true,
    )

    operator fun invoke(params: Params = Params()): Flow<List<Subscription>> =
        repository.getAllSubscriptions().map { list ->
            list
                .filter { sub ->
                    val matchesQuery = params.query.isBlank() ||
                            sub.name.contains(params.query, ignoreCase = true)
                    val matchesCategory = params.filterCategory == null ||
                            sub.category.label == params.filterCategory
                    val matchesActive = !params.activeOnly || sub.isActive
                    matchesQuery && matchesCategory && matchesActive
                }
                .sortedWith(
                    when (params.sortOrder) {
                        SortOrder.NAME         -> compareBy { it.name.lowercase() }
                        SortOrder.COST_HIGH    -> compareByDescending { it.monthlyCost }
                        SortOrder.COST_LOW     -> compareBy { it.monthlyCost }
                        SortOrder.NEXT_BILLING -> compareBy { it.nextBillingDate }
                        SortOrder.CATEGORY     -> compareBy { it.category.label }
                    }
                )
        }
}
