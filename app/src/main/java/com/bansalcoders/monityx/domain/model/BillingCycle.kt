package com.bansalcoders.monityx.domain.model

/**
 * Represents the recurring billing interval of a subscription.
 * [multiplierDays] is used to normalise costs to a monthly equivalent for analytics.
 */
enum class BillingCycle(val label: String, val multiplierDays: Int) {
    WEEKLY(label = "Weekly",  multiplierDays = 7),
    MONTHLY(label = "Monthly", multiplierDays = 30),
    YEARLY(label = "Yearly",  multiplierDays = 365);

    /** Approximate number of billing events per month */
    val perMonth: Double get() = when (this) {
        WEEKLY  -> 365.0 / 7.0 / 12.0
        MONTHLY -> 1.0
        YEARLY  -> 1.0 / 12.0
    }

    companion object {
        fun fromLabel(label: String): BillingCycle =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: MONTHLY
    }
}
