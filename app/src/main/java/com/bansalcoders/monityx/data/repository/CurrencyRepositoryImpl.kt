package com.bansalcoders.monityx.data.repository

import android.util.Log
import com.bansalcoders.monityx.data.local.dao.CurrencyRateDao
import com.bansalcoders.monityx.data.local.entities.CurrencyRateEntity
import com.bansalcoders.monityx.data.remote.api.CurrencyApiService
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [CurrencyRepository] with a network-first, cache-fallback strategy.
 *
 * - Rates are fetched from open.er-api.com and stored locally.
 * - If the network is unavailable, cached rates are used.
 * - Falls back to 1.0 (no conversion) if neither source is available.
 */
@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val apiService: CurrencyApiService,
    private val dao: CurrencyRateDao,
) : CurrencyRepository {

    companion object {
        private const val TAG = "CurrencyRepo"

        /** Hardcoded popular ISO-4217 codes available even without a network call. */
        val SUPPORTED_CURRENCIES = listOf(
            "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "BRL",
            "MXN", "KRW", "SGD", "HKD", "NOK", "SEK", "DKK", "NZD", "ZAR", "RUB",
            "PLN", "TRY", "AED", "SAR", "THB", "IDR", "MYR", "PHP", "CZK", "HUF",
        )
    }

    override suspend fun getExchangeRate(fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return 1.0

        val key = CurrencyRateEntity.key(fromCurrency, toCurrency)
        val cached = dao.getRate(key)
        if (cached != null) return cached.rate

        // Try to derive from reverse rate (target→base)
        val reverseKey = CurrencyRateEntity.key(toCurrency, fromCurrency)
        val reverse = dao.getRate(reverseKey)
        if (reverse != null && reverse.rate != 0.0) return 1.0 / reverse.rate

        // If no cached rate, try refreshing in-line (best-effort)
        try {
            refreshRates(fromCurrency)
            return dao.getRate(key)?.rate ?: 1.0
        } catch (e: Exception) {
            Log.w(TAG, "Currency rate unavailable for $fromCurrency → $toCurrency, using 1.0", e)
            return 1.0
        }
    }

    override suspend fun refreshRates(baseCurrency: String) {
        try {
            val response = apiService.getLatestRates(baseCurrency)
            if (response.isSuccessful) {
                val dto = response.body() ?: return
                if (dto.result != "success") return

                val entities = dto.rates.map { (target, rate) ->
                    CurrencyRateEntity(
                        key = CurrencyRateEntity.key(baseCurrency, target),
                        baseCurrency = baseCurrency,
                        targetCurrency = target,
                        rate = rate,
                    )
                }
                dao.deleteRatesForBase(baseCurrency)
                dao.insertRates(entities)
                Log.d(TAG, "Refreshed ${entities.size} rates for $baseCurrency")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh currency rates", e)
            // Silently fail – stale cache or 1.0 fallback will be used
        }
    }

    override suspend fun getSupportedCurrencies(): List<String> {
        val cached = dao.getRatesForBase("USD").map { it.targetCurrency }
        return if (cached.isNotEmpty()) cached.sorted() else SUPPORTED_CURRENCIES.sorted()
    }
}
