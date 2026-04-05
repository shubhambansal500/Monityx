package com.bansalcoders.monityx.utils

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Currency formatting helpers.
 */
object CurrencyUtils {

    /**
     * Formats an amount with its currency symbol, using the device locale.
     * e.g. formatAmount(9.99, "USD") → "$9.99"
     */
    fun formatAmount(amount: Double, currencyCode: String): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                this.currency = currency
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
            format.format(amount)
        } catch (e: IllegalArgumentException) {
            "$currencyCode ${"%.2f".format(amount)}"
        }
    }

    /**
     * Returns the currency symbol for a given ISO-4217 code.
     * Falls back to the code itself if the symbol is unavailable.
     */
    fun getSymbol(currencyCode: String): String =
        runCatching { Currency.getInstance(currencyCode).symbol }.getOrDefault(currencyCode)

    /**
     * Returns the display name for a given ISO-4217 code.
     */
    fun getDisplayName(currencyCode: String): String =
        runCatching {
            Currency.getInstance(currencyCode).getDisplayName(Locale.getDefault())
        }.getOrDefault(currencyCode)

    /** Formats a compact monthly/yearly cost label. */
    fun formatMonthlyCost(amount: Double, currency: String): String =
        "${formatAmount(amount, currency)}/mo"

    /** All commonly used ISO-4217 codes with display names. */
    val POPULAR_CURRENCIES: List<Pair<String, String>> = listOf(
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "GBP" to "British Pound",
        "JPY" to "Japanese Yen",
        "CAD" to "Canadian Dollar",
        "AUD" to "Australian Dollar",
        "CHF" to "Swiss Franc",
        "CNY" to "Chinese Yuan",
        "INR" to "Indian Rupee",
        "BRL" to "Brazilian Real",
        "MXN" to "Mexican Peso",
        "KRW" to "South Korean Won",
        "SGD" to "Singapore Dollar",
        "HKD" to "Hong Kong Dollar",
        "SEK" to "Swedish Krona",
        "NOK" to "Norwegian Krone",
        "DKK" to "Danish Krone",
        "NZD" to "New Zealand Dollar",
        "ZAR" to "South African Rand",
        "PLN" to "Polish Zloty",
        "AED" to "UAE Dirham",
        "SAR" to "Saudi Riyal",
        "THB" to "Thai Baht",
        "IDR" to "Indonesian Rupiah",
        "MYR" to "Malaysian Ringgit",
        "PHP" to "Philippine Peso",
        "TRY" to "Turkish Lira",
    )
}
