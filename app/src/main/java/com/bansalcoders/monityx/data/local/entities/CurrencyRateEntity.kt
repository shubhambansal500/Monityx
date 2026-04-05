package com.bansalcoders.monityx.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached exchange rate.
 * [baseCurrency] + [targetCurrency] form the logical primary key.
 * Stored as a single composite string key to simplify upsert logic.
 */
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey
    val key: String,           // "$baseCurrency_$targetCurrency"
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun key(base: String, target: String) = "${base}_${target}"
    }
}
