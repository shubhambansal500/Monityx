package com.bansalcoders.monityx.data.remote.api

import com.bansalcoders.monityx.data.remote.dto.CurrencyRateDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for the Open Exchange Rates (open.er-api.com) free tier.
 *
 * No API key is required for the free tier – just hit the endpoint.
 * API key support (via header injection in NetworkModule) is available for premium.
 *
 * Endpoint: GET https://open.er-api.com/v6/latest/{baseCurrency}
 */
interface CurrencyApiService {
    @GET("latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): Response<CurrencyRateDto>
}
