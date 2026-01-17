package com.signalsynthesis.data.provider.alpaca

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AlpacaService {
    @GET("v2/stocks/{symbol}/quotes/latest")
    suspend fun getLatestQuote(
        @Path("symbol") symbol: String
    ): AlpacaLatestQuoteResponse

    @GET("v2/stocks/{symbol}/bars")
    suspend fun getBars(
        @Path("symbol") symbol: String,
        @Query("timeframe") timeframe: String,
        @Query("start") start: String,
        @Query("end") end: String?,
        @Query("limit") limit: Int = 10000,
        @Query("adjustment") adjustment: String = "all"
    ): AlpacaBarsResponse

    @GET("v1/assets/{symbol}")
    suspend fun getAsset(
        @Path("symbol") symbol: String
    ): AlpacaAssetResponse

    companion object {
        private const val BASE_URL = "https://data.alpaca.markets/"

        fun create(apiKey: String, secretKey: String, client: OkHttpClient? = null): AlpacaService {
            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("APCA-API-KEY-ID", apiKey)
                    .addHeader("APCA-API-SECRET-KEY", secretKey)
                    .build()
                chain.proceed(request)
            }

            val okHttpClient = (client ?: OkHttpClient.Builder().build())
                .newBuilder()
                .addInterceptor(authInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(okHttpClient)
                .build()

            return retrofit.create(AlpacaService::class.java)
        }
    }
}

data class AlpacaLatestQuoteResponse(
    val symbol: String? = null,
    val quote: AlpacaQuote? = null
)

data class AlpacaQuote(
    @Json(name = "ap") val askPrice: Double? = null,
    @Json(name = "bp") val bidPrice: Double? = null,
    @Json(name = "as") val askSize: Long? = null,
    @Json(name = "bs") val bidSize: Long? = null,
    @Json(name = "t") val timestamp: String? = null
)

data class AlpacaBarsResponse(
    val symbol: String? = null,
    val bars: List<AlpacaBar>? = null,
    val next_page_token: String? = null
)

data class AlpacaBar(
    @Json(name = "t") val timestamp: String? = null,
    @Json(name = "o") val open: Double? = null,
    @Json(name = "h") val high: Double? = null,
    @Json(name = "l") val low: Double? = null,
    @Json(name = "c") val close: Double? = null,
    @Json(name = "v") val volume: Long? = null,
    @Json(name = "n") val tradeCount: Int? = null,
    @Json(name = "vw") val vwap: Double? = null
)

data class AlpacaAssetResponse(
    val id: String? = null,
    @Json(name = "class") val assetClass: String? = null,
    val exchange: String? = null,
    val symbol: String? = null,
    val name: String? = null,
    val status: String? = null,
    val tradable: Boolean? = null,
    val marginable: Boolean? = null,
    val maintenance_margin_requirement: Double? = null,
    val shortable: Boolean? = null,
    val easy_to_borrow: Boolean? = null,
    val fractionable: Boolean? = null,
    val min_order_size: String? = null,
    val min_trade_increment: String? = null,
    val price_increment: String? = null
)
