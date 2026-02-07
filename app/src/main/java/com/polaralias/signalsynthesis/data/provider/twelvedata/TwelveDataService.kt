package com.polaralias.signalsynthesis.data.provider.twelvedata

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface TwelveDataService {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): TwelveDataQuote

    @GET("time_series")
    suspend fun getTimeSeries(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("outputsize") outputsize: Int,
        @Query("apikey") apiKey: String
    ): TwelveDataTimeSeries

    @GET("profile")
    suspend fun getProfile(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): TwelveDataProfile

    @GET("statistics")
    suspend fun getStatistics(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): TwelveDataStatistics

    companion object {
        private const val BASE_URL = "https://api.twelvedata.com/"

        fun create(client: OkHttpClient? = null): TwelveDataService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client ?: OkHttpClient())
                .build()
            return retrofit.create(TwelveDataService::class.java)
        }
    }
}

data class TwelveDataQuote(
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null,
    val symbol: String? = null,
    val name: String? = null,
    @Json(name = "price") val priceRaw: Any? = null,
    @Json(name = "percent_change") val percentChangeRaw: Any? = null,
    @Json(name = "volume") val volumeRaw: Any? = null,
    @Json(name = "timestamp") val timestampRaw: Any? = null
)

data class TwelveDataTimeSeries(
    val values: List<TwelveDataBar>? = null,
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null
)

data class TwelveDataBar(
    val datetime: String? = null,
    val open: Any? = null,
    val high: Any? = null,
    val low: Any? = null,
    val close: Any? = null,
    val volume: Any? = null
)

data class TwelveDataProfile(
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null,
    val name: String? = null,
    val exchange: String? = null,
    val sector: String? = null,
    val industry: String? = null,
    val description: String? = null
)

data class TwelveDataStatistics(
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null,
    val valuations_metrics: TwelveDataValuations? = null,
    val dividends_and_splits: TwelveDataDividends? = null
)

data class TwelveDataValuations(
    @Json(name = "market_capitalization") val marketCapRaw: Any? = null,
    @Json(name = "pe_ratio") val peRatioRaw: Any? = null,
    @Json(name = "price_to_book") val pbRatioRaw: Any? = null
)

data class TwelveDataDividends(
    @Json(name = "dividend_yield") val dividendYieldRaw: Any? = null
)
