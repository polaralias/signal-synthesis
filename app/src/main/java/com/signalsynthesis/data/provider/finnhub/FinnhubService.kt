package com.signalsynthesis.data.provider.finnhub

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubService {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): FinnhubQuoteResponse

    @GET("stock/candle")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") fromEpochSeconds: Long,
        @Query("to") toEpochSeconds: Long,
        @Query("token") token: String
    ): FinnhubCandleResponse

    @GET("stock/profile2")
    suspend fun getProfile(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): FinnhubProfileResponse

    @GET("stock/metric")
    suspend fun getMetrics(
        @Query("symbol") symbol: String,
        @Query("metric") metric: String = "all",
        @Query("token") token: String
    ): FinnhubMetricsResponse

    @GET("news-sentiment")
    suspend fun getSentiment(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): FinnhubSentimentResponse

    companion object {
        private const val BASE_URL = "https://finnhub.io/api/v1/"

        fun create(client: OkHttpClient? = null): FinnhubService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client ?: OkHttpClient())
                .build()
            return retrofit.create(FinnhubService::class.java)
        }
    }
}

data class FinnhubQuoteResponse(
    @Json(name = "c") val currentPrice: Double? = null,
    @Json(name = "t") val timestamp: Long? = null,
    @Json(name = "v") val volume: Double? = null
)

data class FinnhubCandleResponse(
    @Json(name = "c") val close: List<Double>? = null,
    @Json(name = "h") val high: List<Double>? = null,
    @Json(name = "l") val low: List<Double>? = null,
    @Json(name = "o") val open: List<Double>? = null,
    @Json(name = "t") val time: List<Long>? = null,
    @Json(name = "v") val volume: List<Long>? = null,
    @Json(name = "s") val status: String? = null
)

data class FinnhubProfileResponse(
    val name: String? = null,
    @Json(name = "finnhubIndustry") val industry: String? = null,
    val ticker: String? = null
)

data class FinnhubMetricsResponse(
    val metric: FinnhubMetricData? = null
)

data class FinnhubMetricData(
    @Json(name = "marketCapitalization") val marketCapitalization: Double? = null,
    @Json(name = "peTTM") val peTtm: Double? = null,
    @Json(name = "epsTTM") val epsTtm: Double? = null
)

data class FinnhubSentimentResponse(
    val sentiment: FinnhubSentimentBreakdown? = null,
    val companyNewsScore: Double? = null
)

data class FinnhubSentimentBreakdown(
    val bullishPercent: Double? = null,
    val bearishPercent: Double? = null
)
