package com.polaralias.signalsynthesis.data.provider.polygon

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MassiveService {
    @GET("v2/snapshot/locale/us/markets/stocks/tickers/{ticker}")
    suspend fun getSnapshot(
        @Path("ticker") ticker: String,
        @Query("apiKey") apiKey: String
    ): MassiveSnapshotResponse

    @GET("v2/aggs/ticker/{stocksTicker}/range/{multiplier}/{timespan}/{from}/{to}")
    suspend fun getAggregates(
        @Path("stocksTicker") ticker: String,
        @Path("multiplier") multiplier: Int,
        @Path("timespan") timespan: String,
        @Path("from") from: String,
        @Path("to") to: String,
        @Query("adjusted") adjusted: Boolean = true,
        @Query("sort") sort: String = "asc",
        @Query("limit") limit: Int = 50000,
        @Query("apiKey") apiKey: String
    ): MassiveAggregatesResponse

    @GET("v3/reference/tickers/{ticker}")
    suspend fun getTickerDetails(
        @Path("ticker") ticker: String,
        @Query("apiKey") apiKey: String
    ): MassiveTickerDetailsResponse

    @GET("v3/reference/tickers")
    suspend fun listTickers(
        @Query("search") search: String? = null,
        @Query("market") market: String = "stocks",
        @Query("active") active: Boolean = true,
        @Query("type") type: String? = "CS",
        @Query("limit") limit: Int = 100,
        @Query("apiKey") apiKey: String
    ): MassiveTickersResponse

    companion object {
        private const val BASE_URL = "https://api.polygon.io/"

        fun create(client: OkHttpClient? = null): MassiveService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client ?: OkHttpClient())
                .build()
            return retrofit.create(MassiveService::class.java)
        }
    }
}

data class MassiveTickersResponse(
    val status: String? = null,
    val results: List<MassiveTickerResult>? = null,
    val count: Int? = null
)

data class MassiveTickerResult(
    val ticker: String? = null,
    val name: String? = null,
    val market: String? = null,
    val locale: String? = null,
    val type: String? = null,
    val active: Boolean? = null,
    val currency_name: String? = null
)

data class MassiveSnapshotResponse(
    val status: String? = null,
    val ticker: MassiveTickerSnapshot? = null
)

data class MassiveTickerSnapshot(
    val ticker: String? = null,
    val day: MassiveDayData? = null,
    val lastTrade: MassiveTradeData? = null,
    val min: MassiveMinData? = null,
    val todaysChangePerc: Double? = null
)

data class MassiveDayData(
    @Json(name = "c") val close: Double? = null,
    @Json(name = "h") val high: Double? = null,
    @Json(name = "l") val low: Double? = null,
    @Json(name = "o") val open: Double? = null,
    @Json(name = "v") val volume: Long? = null,
    @Json(name = "vw") val vwap: Double? = null
)

data class MassiveTradeData(
    @Json(name = "p") val price: Double? = null,
    @Json(name = "t") val timestamp: Long? = null
)

data class MassiveMinData(
    @Json(name = "av") val accumulatedVolume: Long? = null
)

data class MassiveAggregatesResponse(
    val status: String? = null,
    val ticker: String? = null,
    val resultsCount: Int? = null,
    val results: List<MassiveAggregate>? = null
)

data class MassiveAggregate(
    @Json(name = "c") val close: Double? = null,
    @Json(name = "h") val high: Double? = null,
    @Json(name = "l") val low: Double? = null,
    @Json(name = "o") val open: Double? = null,
    @Json(name = "t") val timestamp: Long? = null,
    @Json(name = "v") val volume: Long? = null,
    @Json(name = "vw") val vwap: Double? = null,
    @Json(name = "n") val transactions: Int? = null
)

data class MassiveTickerDetailsResponse(
    val status: String? = null,
    val results: MassiveTickerDetails? = null
)

data class MassiveTickerDetails(
    val ticker: String? = null,
    val name: String? = null,
    val market: String? = null,
    val locale: String? = null,
    val primary_exchange: String? = null,
    val type: String? = null,
    val active: Boolean? = null,
    val currency_name: String? = null,
    val cik: String? = null,
    val composite_figi: String? = null,
    val share_class_figi: String? = null,
    val market_cap: Double? = null,
    val phone_number: String? = null,
    val address: MassiveAddress? = null,
    val description: String? = null,
    val sic_code: String? = null,
    val sic_description: String? = null,
    val ticker_root: String? = null,
    val homepage_url: String? = null,
    val total_employees: Int? = null,
    val list_date: String? = null,
    val branding: MassiveBranding? = null,
    val share_class_shares_outstanding: Long? = null,
    val weighted_shares_outstanding: Long? = null,
    val round_lot: Int? = null
)

data class MassiveAddress(
    val address1: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postal_code: String? = null
)

data class MassiveBranding(
    val logo_url: String? = null,
    val icon_url: String? = null
)
