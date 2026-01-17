package com.polaralias.signalsynthesis.data.provider.fmp

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FmpService {
    @GET("v3/quote/{symbol}")
    suspend fun getQuote(
        @Path("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): List<FmpQuote>

    @GET("v3/historical-chart/{timeframe}/{symbol}")
    suspend fun getHistoricalChart(
        @Path("timeframe") timeframe: String,
        @Path("symbol") symbol: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("apikey") apiKey: String
    ): List<FmpChartBar>

    @GET("v3/profile/{symbol}")
    suspend fun getProfile(
        @Path("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): List<FmpProfile>

    @GET("v3/key-metrics/{symbol}")
    suspend fun getKeyMetrics(
        @Path("symbol") symbol: String,
        @Query("limit") limit: Int = 1,
        @Query("apikey") apiKey: String
    ): List<FmpKeyMetrics>

    @GET("v3/stock-news-sentiments-rss-feed")
    suspend fun getNewsSentiment(
        @Query("tickers") tickers: String,
        @Query("page") page: Int = 0,
        @Query("apikey") apiKey: String
    ): List<FmpNewsSentiment>

    companion object {
        private const val BASE_URL = "https://financialmodelingprep.com/api/"

        fun create(client: OkHttpClient? = null): FmpService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client ?: OkHttpClient())
                .build()
            return retrofit.create(FmpService::class.java)
        }
    }
}

data class FmpQuote(
    val symbol: String? = null,
    val name: String? = null,
    val price: Double? = null,
    val changesPercentage: Double? = null,
    val change: Double? = null,
    val dayLow: Double? = null,
    val dayHigh: Double? = null,
    val yearHigh: Double? = null,
    val yearLow: Double? = null,
    val marketCap: Long? = null,
    val priceAvg50: Double? = null,
    val priceAvg200: Double? = null,
    val volume: Long? = null,
    val avgVolume: Long? = null,
    val exchange: String? = null,
    val open: Double? = null,
    val previousClose: Double? = null,
    val eps: Double? = null,
    val pe: Double? = null,
    val earningsAnnouncement: String? = null,
    val sharesOutstanding: Long? = null,
    val timestamp: Long? = null
)

data class FmpChartBar(
    val date: String? = null,
    val open: Double? = null,
    val low: Double? = null,
    val high: Double? = null,
    val close: Double? = null,
    val volume: Long? = null
)

data class FmpProfile(
    val symbol: String? = null,
    val price: Double? = null,
    val beta: Double? = null,
    val volAvg: Long? = null,
    val mktCap: Long? = null,
    val lastDiv: Double? = null,
    val range: String? = null,
    val changes: Double? = null,
    val companyName: String? = null,
    val currency: String? = null,
    val cik: String? = null,
    val isin: String? = null,
    val cusip: String? = null,
    val exchange: String? = null,
    val exchangeShortName: String? = null,
    val industry: String? = null,
    val website: String? = null,
    val description: String? = null,
    val ceo: String? = null,
    val sector: String? = null,
    val country: String? = null,
    val fullTimeEmployees: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val dcfDiff: Double? = null,
    val dcf: Double? = null,
    val image: String? = null,
    val ipoDate: String? = null,
    val defaultImage: Boolean? = null,
    val isEtf: Boolean? = null,
    val isActivelyTrading: Boolean? = null,
    val isAdr: Boolean? = null,
    val isFund: Boolean? = null
)

data class FmpKeyMetrics(
    val symbol: String? = null,
    val date: String? = null,
    val calendarYear: String? = null,
    val period: String? = null,
    val revenuePerShare: Double? = null,
    val netIncomePerShare: Double? = null,
    val operatingCashFlowPerShare: Double? = null,
    val freeCashFlowPerShare: Double? = null,
    val cashPerShare: Double? = null,
    val bookValuePerShare: Double? = null,
    val tangibleBookValuePerShare: Double? = null,
    val shareholdersEquityPerShare: Double? = null,
    val interestDebtPerShare: Double? = null,
    val marketCap: Long? = null,
    val enterpriseValue: Long? = null,
    val peRatio: Double? = null,
    val priceToSalesRatio: Double? = null,
    val pocfratio: Double? = null,
    val pfcfRatio: Double? = null,
    val pbRatio: Double? = null,
    val ptbRatio: Double? = null,
    val evToSales: Double? = null,
    val enterpriseValueOverEBITDA: Double? = null,
    val evToOperatingCashFlow: Double? = null,
    val evToFreeCashFlow: Double? = null,
    val earningsYield: Double? = null,
    val freeCashFlowYield: Double? = null,
    val debtToEquity: Double? = null,
    val debtToAssets: Double? = null,
    val netDebtToEBITDA: Double? = null,
    val currentRatio: Double? = null,
    val interestCoverage: Double? = null,
    val incomeQuality: Double? = null,
    val dividendYield: Double? = null,
    val payoutRatio: Double? = null,
    val salesGeneralAndAdministrativeToRevenue: Double? = null,
    val researchAndDevelopementToRevenue: Double? = null,
    val intangiblesToTotalAssets: Double? = null,
    val capexToOperatingCashFlow: Double? = null,
    val capexToRevenue: Double? = null,
    val capexToDepreciation: Double? = null,
    val stockBasedCompensationToRevenue: Double? = null,
    val grahamNumber: Double? = null,
    val roic: Double? = null,
    val returnOnTangibleAssets: Double? = null,
    val grahamNetNet: Double? = null,
    val workingCapital: Long? = null,
    val tangibleAssetValue: Long? = null,
    val netCurrentAssetValue: Long? = null,
    val investedCapital: Long? = null,
    val averageReceivables: Long? = null,
    val averagePayables: Long? = null,
    val averageInventory: Long? = null,
    val daysSalesOutstanding: Double? = null,
    val daysPayablesOutstanding: Double? = null,
    val daysOfInventoryOnHand: Double? = null,
    val receivablesTurnover: Double? = null,
    val payablesTurnover: Double? = null,
    val inventoryTurnover: Double? = null,
    val roe: Double? = null,
    val capexPerShare: Double? = null
)

data class FmpNewsSentiment(
    val symbol: String? = null,
    val publishedDate: String? = null,
    val title: String? = null,
    val image: String? = null,
    val site: String? = null,
    val text: String? = null,
    val url: String? = null,
    val sentiment: String? = null,
    val sentimentScore: Double? = null
)
