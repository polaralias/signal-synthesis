package com.signalsynthesis.data.provider.finnhub

import com.signalsynthesis.domain.model.CompanyProfile
import com.signalsynthesis.domain.model.DailyBar
import com.signalsynthesis.domain.model.FinancialMetrics
import com.signalsynthesis.domain.model.IntradayBar
import com.signalsynthesis.domain.model.Quote
import com.signalsynthesis.domain.model.SentimentData
import com.signalsynthesis.domain.provider.DailyProvider
import com.signalsynthesis.domain.provider.IntradayProvider
import com.signalsynthesis.domain.provider.MetricsProvider
import com.signalsynthesis.domain.provider.ProfileProvider
import com.signalsynthesis.domain.provider.QuoteProvider
import com.signalsynthesis.domain.provider.SentimentProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.max

class FinnhubMarketDataProvider(
    private val apiKey: String,
    private val service: FinnhubService = FinnhubService.create(),
    private val clock: Clock = Clock.systemUTC()
) : QuoteProvider,
    IntradayProvider,
    DailyProvider,
    ProfileProvider,
    MetricsProvider,
    SentimentProvider {

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        if (symbols.isEmpty()) return emptyMap()
        return symbols.mapNotNull { symbol ->
            val response = service.getQuote(symbol, apiKey)
            response.toQuote(symbol)
        }.associateBy { it.symbol }
    }

    override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        val candle = fetchCandles(symbol, days, resolution = "5")
        return candle.toIntradayBars()
    }

    override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        val candle = fetchCandles(symbol, days, resolution = "D")
        return candle.toDailyBars()
    }

    override suspend fun getProfile(symbol: String): CompanyProfile? {
        if (symbol.isBlank()) return null
        val response = service.getProfile(symbol, apiKey)
        return CompanyProfile(
            name = response.name ?: symbol,
            sector = null,
            industry = response.industry,
            description = null
        )
    }

    override suspend fun getMetrics(symbol: String): FinancialMetrics? {
        if (symbol.isBlank()) return null
        val response = service.getMetrics(symbol, token = apiKey)
        val metric = response.metric ?: return null
        return FinancialMetrics(
            marketCap = metric.marketCapitalization?.times(1_000_000)?.toLong(),
            peRatio = metric.peTtm,
            eps = metric.epsTtm
        )
    }

    override suspend fun getSentiment(symbol: String): SentimentData? {
        if (symbol.isBlank()) return null
        val response = service.getSentiment(symbol, apiKey)
        val score = response.score()
        if (score == null) return null
        return SentimentData(
            score = score,
            label = score.toLabel()
        )
    }

    private suspend fun fetchCandles(
        symbol: String,
        days: Int,
        resolution: String
    ): FinnhubCandleResponse {
        val now = Instant.now(clock).epochSecond
        val from = now - max(days, 1) * SECONDS_PER_DAY
        return service.getCandles(
            symbol = symbol,
            resolution = resolution,
            fromEpochSeconds = from,
            toEpochSeconds = now,
            token = apiKey
        )
    }

    private fun FinnhubQuoteResponse.toQuote(symbol: String): Quote? {
        val price = currentPrice ?: return null
        val timestampSeconds = timestamp ?: return null
        return Quote(
            symbol = symbol,
            price = price,
            volume = volume?.toLong() ?: 0L,
            timestamp = Instant.ofEpochSecond(timestampSeconds)
        )
    }

    private fun FinnhubCandleResponse.toIntradayBars(): List<IntradayBar> {
        if (status != "ok") return emptyList()
        val times = time.orEmpty()
        val opens = open.orEmpty()
        val highs = high.orEmpty()
        val lows = low.orEmpty()
        val closes = close.orEmpty()
        val volumes = volume.orEmpty()
        val count = minOf(times.size, opens.size, highs.size, lows.size, closes.size)
        return (0 until count).mapNotNull { index ->
            val epoch = times[index]
            val volumeValue = volumes.getOrNull(index) ?: 0L
            IntradayBar(
                time = Instant.ofEpochSecond(epoch),
                open = opens[index],
                high = highs[index],
                low = lows[index],
                close = closes[index],
                volume = volumeValue
            )
        }
    }

    private fun FinnhubCandleResponse.toDailyBars(): List<DailyBar> {
        if (status != "ok") return emptyList()
        val times = time.orEmpty()
        val opens = open.orEmpty()
        val highs = high.orEmpty()
        val lows = low.orEmpty()
        val closes = close.orEmpty()
        val volumes = volume.orEmpty()
        val count = minOf(times.size, opens.size, highs.size, lows.size, closes.size)
        return (0 until count).mapNotNull { index ->
            val epoch = times[index]
            val date = Instant.ofEpochSecond(epoch)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            val volumeValue = volumes.getOrNull(index) ?: 0L
            DailyBar(
                date = date,
                open = opens[index],
                high = highs[index],
                low = lows[index],
                close = closes[index],
                volume = volumeValue
            )
        }
    }

    private fun FinnhubSentimentResponse.score(): Double? {
        val bullish = sentiment?.bullishPercent
        val bearish = sentiment?.bearishPercent
        if (bullish == null || bearish == null) return null
        val raw = (bullish - bearish) / 100.0
        return raw.coerceIn(-1.0, 1.0)
    }

    private fun Double.toLabel(): String {
        return when {
            this > 0.2 -> "Bullish"
            this < -0.2 -> "Bearish"
            else -> "Neutral"
        }
    }

    companion object {
        private const val SECONDS_PER_DAY = 86_400L
    }
}
