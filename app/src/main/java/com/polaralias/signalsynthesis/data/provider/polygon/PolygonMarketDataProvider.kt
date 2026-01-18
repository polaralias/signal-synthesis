package com.polaralias.signalsynthesis.data.provider.polygon

import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.domain.provider.DailyProvider
import com.polaralias.signalsynthesis.domain.provider.IntradayProvider
import com.polaralias.signalsynthesis.domain.provider.MetricsProvider
import com.polaralias.signalsynthesis.domain.provider.ProfileProvider
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import com.polaralias.signalsynthesis.domain.provider.SentimentProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class PolygonMarketDataProvider(
    private val apiKey: String,
    private val service: PolygonService = PolygonService.create(),
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
            try {
                val response = service.getSnapshot(symbol, apiKey)
                response.toQuote()
            } catch (e: Exception) {
                null
            }
        }.associateBy { it.symbol }
    }

    override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        return try {
            val (from, to) = getDateRange(days)
            val response = service.getAggregates(
                ticker = symbol,
                multiplier = 5,
                timespan = "minute",
                from = from,
                to = to,
                apiKey = apiKey
            )
            response.toIntradayBars()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        return try {
            val (from, to) = getDateRange(days)
            val response = service.getAggregates(
                ticker = symbol,
                multiplier = 1,
                timespan = "day",
                from = from,
                to = to,
                apiKey = apiKey
            )
            response.toDailyBars()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getProfile(symbol: String): CompanyProfile? {
        if (symbol.isBlank()) return null
        return try {
            val response = service.getTickerDetails(symbol, apiKey)
            response.results?.let {
                CompanyProfile(
                    name = it.name ?: symbol,
                    sector = it.sic_description,
                    industry = it.type,
                    description = it.description
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMetrics(symbol: String): FinancialMetrics? {
        if (symbol.isBlank()) return null
        return try {
            val response = service.getTickerDetails(symbol, apiKey)
            response.results?.let {
                FinancialMetrics(
                    marketCap = it.market_cap?.toLong(),
                    peRatio = null, // Polygon doesn't provide PE ratio in ticker details
                    eps = null // Polygon doesn't provide EPS in ticker details
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getSentiment(symbol: String): SentimentData? {
        // Polygon doesn't provide sentiment data directly
        return null
    }

    private fun getDateRange(days: Int): Pair<String, String> {
        val now = LocalDate.now(clock)
        val from = now.minusDays(days.toLong())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return Pair(from.format(formatter), now.format(formatter))
    }

    private fun PolygonSnapshotResponse.toQuote(): Quote? {
        val tickerData = ticker ?: return null
        val symbol = tickerData.ticker ?: return null
        val price = tickerData.lastTrade?.price ?: tickerData.day?.close ?: return null
        val volume = tickerData.day?.volume ?: tickerData.min?.accumulatedVolume ?: 0L
        val timestamp = tickerData.lastTrade?.timestamp?.let {
            Instant.ofEpochMilli(it)
        } ?: Instant.now(clock)
        
        return Quote(
            symbol = symbol,
            price = price,
            volume = volume,
            timestamp = timestamp,
            changePercent = tickerData.todaysChangePerc
        )
    }

    private fun PolygonAggregatesResponse.toIntradayBars(): List<IntradayBar> {
        if (status != "OK") return emptyList()
        return results?.mapNotNull { agg ->
            val timestamp = agg.timestamp ?: return@mapNotNull null
            val open = agg.open ?: return@mapNotNull null
            val high = agg.high ?: return@mapNotNull null
            val low = agg.low ?: return@mapNotNull null
            val close = agg.close ?: return@mapNotNull null
            val volume = agg.volume ?: 0L
            
            IntradayBar(
                time = Instant.ofEpochMilli(timestamp),
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume
            )
        } ?: emptyList()
    }

    private fun PolygonAggregatesResponse.toDailyBars(): List<DailyBar> {
        if (status != "OK") return emptyList()
        return results?.mapNotNull { agg ->
            val timestamp = agg.timestamp ?: return@mapNotNull null
            val open = agg.open ?: return@mapNotNull null
            val high = agg.high ?: return@mapNotNull null
            val low = agg.low ?: return@mapNotNull null
            val close = agg.close ?: return@mapNotNull null
            val volume = agg.volume ?: 0L
            
            val date = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            
            DailyBar(
                date = date,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume
            )
        } ?: emptyList()
    }
}
