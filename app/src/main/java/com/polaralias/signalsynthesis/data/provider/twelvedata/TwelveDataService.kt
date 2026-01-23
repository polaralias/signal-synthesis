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
    val symbol: String? = null,
    val name: String? = null,
    val price: Double? = null,
    @Json(name = "percent_change") val percentChange: Double? = null,
    val volume: Long? = null,
    val timestamp: Long? = null
)

data class TwelveDataTimeSeries(
    val values: List<TwelveDataBar> = emptyList(),
    val status: String? = null
)

data class TwelveDataBar(
    val datetime: String? = null,
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val close: Double? = null,
    val volume: Long? = null
)

data class TwelveDataProfile(
    val name: String? = null,
    val exchange: String? = null,
    val sector: String? = null,
    val industry: String? = null,
    val description: String? = null
)

data class TwelveDataStatistics(
    val valuations_metrics: TwelveDataValuations? = null,
    val dividends_and_splits: TwelveDataDividends? = null
)

data class TwelveDataValuations(
    @Json(name = "market_capitalization") val marketCap: Long? = null,
    @Json(name = "pe_ratio") val peRatio: Double? = null,
    @Json(name = "price_to_book") val pbRatio: Double? = null
)

data class TwelveDataDividends(
    @Json(name = "dividend_yield") val dividendYield: Double? = null
)
