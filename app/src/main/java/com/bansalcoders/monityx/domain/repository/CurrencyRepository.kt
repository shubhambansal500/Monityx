package com.bansalcoders.monityx.domain.repository

/**
 * Domain-layer contract for currency exchange rates.
 * Implementations may fetch from remote API and/or use local cache.
 */
interface CurrencyRepository {

    /**
     * Returns the exchange rate from [fromCurrency] to [toCurrency].
     * Falls back to 1.0 if the rate is unavailable.
     */
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String): Double

    /**
     * Refreshes rates from the network.
     * Should be called at most once per session / WorkManager periodic task.
     */
    suspend fun refreshRates(baseCurrency: String)

    /** Returns the set of supported ISO-4217 currency codes. */
    suspend fun getSupportedCurrencies(): List<String>
}
