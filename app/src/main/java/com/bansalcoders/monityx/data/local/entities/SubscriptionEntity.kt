package com.bansalcoders.monityx.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bansalcoders.monityx.domain.model.BillingCycle
import com.bansalcoders.monityx.domain.model.Category
import com.bansalcoders.monityx.domain.model.Subscription
import java.time.LocalDate

/**
 * Room persistence model for a subscription.
 * LocalDate fields are stored as Long (epoch day) via [com.bansalcoders.monityx.data.local.database.Converters].
 */
@Entity(
    tableName = "subscriptions",
    indices = [Index(value = ["nextBillingDate"]), Index(value = ["isActive"])],
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val providerKey: String,
    val category: String,          // Category.name stored as String
    val cost: Double,
    val currency: String,
    val billingCycle: String,      // BillingCycle.name stored as String
    val startDate: Long,           // LocalDate.toEpochDay()
    val nextBillingDate: Long,     // LocalDate.toEpochDay()
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val sharedWith: Int = 1,
) {
    fun toDomain(): Subscription = Subscription(
        id              = id,
        name            = name,
        providerKey     = providerKey,
        category        = runCatching { Category.valueOf(category) }.getOrDefault(Category.OTHER),
        cost            = cost,
        currency        = currency,
        billingCycle    = runCatching { BillingCycle.valueOf(billingCycle) }.getOrDefault(BillingCycle.MONTHLY),
        startDate       = LocalDate.ofEpochDay(startDate),
        nextBillingDate = LocalDate.ofEpochDay(nextBillingDate),
        isActive        = isActive,
        notes           = notes,
        createdAt       = createdAt,
        sharedWith      = sharedWith,
    )

    companion object {
        fun fromDomain(s: Subscription): SubscriptionEntity = SubscriptionEntity(
            id              = s.id,
            name            = s.name,
            providerKey     = s.providerKey,
            category        = s.category.name,
            cost            = s.cost,
            currency        = s.currency,
            billingCycle    = s.billingCycle.name,
            startDate       = s.startDate.toEpochDay(),
            nextBillingDate = s.nextBillingDate.toEpochDay(),
            isActive        = s.isActive,
            notes           = s.notes,
            createdAt       = s.createdAt,
            sharedWith      = s.sharedWith,
        )
    }
}
