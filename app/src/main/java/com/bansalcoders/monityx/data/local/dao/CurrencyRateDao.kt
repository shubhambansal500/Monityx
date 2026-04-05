package com.bansalcoders.monityx.data.local.dao

import androidx.room.*
import com.bansalcoders.monityx.data.local.entities.CurrencyRateEntity

/** Room DAO for cached exchange rates. */
@Dao
interface CurrencyRateDao {

    @Query("SELECT * FROM currency_rates WHERE key = :key LIMIT 1")
    suspend fun getRate(key: String): CurrencyRateEntity?

    @Query("SELECT * FROM currency_rates WHERE baseCurrency = :base")
    suspend fun getRatesForBase(base: String): List<CurrencyRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<CurrencyRateEntity>)

    @Query("DELETE FROM currency_rates WHERE baseCurrency = :base")
    suspend fun deleteRatesForBase(base: String)
}
