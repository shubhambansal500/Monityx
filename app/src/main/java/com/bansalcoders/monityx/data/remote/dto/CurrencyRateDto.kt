package com.bansalcoders.monityx.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for the open.er-api.com /v6/latest/{base} response.
 * Only the fields we actually use are mapped; the rest are ignored by Gson.
 */
data class CurrencyRateDto(
    @SerializedName("result")
    val result: String = "",           // "success" | "error"

    @SerializedName("base_code")
    val baseCode: String = "",

    @SerializedName("rates")
    val rates: Map<String, Double> = emptyMap(),

    @SerializedName("time_last_update_utc")
    val lastUpdatedUtc: String = "",
)
