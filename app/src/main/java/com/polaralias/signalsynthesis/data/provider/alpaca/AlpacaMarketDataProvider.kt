package com.polaralias.signalsynthesis.data.provider.alpaca

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

class AlpacaMarketDataProvider(
    private val apiKey: String,
    private val secretKey: String,
    private val service: AlpacaService = AlpacaService.create(apiKey, secretKey),
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
            val response = service.getLatestQuote(symbol)
            response.toQuote()
        }.associateBy { it.symbol }
    }

    override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        val (start, end) = getDateTimeRange(days)
        val response = service.getBars(
            symbol = symbol,
            timeframe = "5Min",
            start = start,
            end = end
        )
        return response.toIntradayBars()
    }

    override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        val (start, end) = getDateTimeRange(days)
        val response = service.getBars(
            symbol = symbol,
            timeframe = "1Day",
            start = start,
            end = end
        )
        return response.toDailyBars()
    }

    override suspend fun getProfile(symbol: String): CompanyProfile? {
        if (symbol.isBlank()) return null
        val response = service.getAsset(symbol)
        return CompanyProfile(
            name = response.name ?: symbol,
            sector = null,
            industry = response.assetClass,
            description = null
        )
    }

    override suspend fun getMetrics(symbol: String): FinancialMetrics? {
        // Alpaca doesn't provide fundamental metrics
        return null
    }

    override suspend fun getSentiment(symbol: String): SentimentData? {
        // Alpaca doesn't provide sentiment data
        return null
    }

    private fun getDateTimeRange(days: Int): Pair<String, String> {
        val now = Instant.now(clock)
        val start = now.minusSeconds(days.toLong() * 86400L)
        val formatter = DateTimeFormatter.ISO_INSTANT
        return Pair(
            formatter.format(start),
            formatter.format(now)
        )
    }

    private fun AlpacaLatestQuoteResponse.toQuote(): Quote? {
        val sym = symbol ?: return null
        val quoteData = quote ?: return null
        
        // Use mid-point of bid-ask as the price
        val ask = quoteData.askPrice
        val bid = quoteData.bidPrice
        val price = when {
            ask != null && bid != null -> (ask + bid) / 2.0
            ask != null -> ask
            bid != null -> bid
            else -> return null
        }
        
        // Use bid+ask size as volume proxy
        val volume = (quoteData.askSize ?: 0L) + (quoteData.bidSize ?: 0L)
        
        val timestamp = quoteData.timestamp?.let {
            try {
                Instant.parse(it)
            } catch (e: Exception) {
                Instant.now(clock)
            }
        } ?: Instant.now(clock)
        
        return Quote(
            symbol = sym,
            price = price,
            volume = volume,
            timestamp = timestamp
        )
    }

    private fun AlpacaBarsResponse.toIntradayBars(): List<IntradayBar> {
        return bars?.mapNotNull { bar ->
            val timestamp = bar.timestamp ?: return@mapNotNull null
            val open = bar.open ?: return@mapNotNull null
            val high = bar.high ?: return@mapNotNull null
            val low = bar.low ?: return@mapNotNull null
            val close = bar.close ?: return@mapNotNull null
            val volume = bar.volume ?: 0L
            
            val time = try {
                Instant.parse(timestamp)
            } catch (e: Exception) {
                return@mapNotNull null
            }
            
            IntradayBar(
                time = time,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume
            )
        } ?: emptyList()
    }

    private fun AlpacaBarsResponse.toDailyBars(): List<DailyBar> {
        return bars?.mapNotNull { bar ->
            val timestamp = bar.timestamp ?: return@mapNotNull null
            val open = bar.open ?: return@mapNotNull null
            val high = bar.high ?: return@mapNotNull null
            val low = bar.low ?: return@mapNotNull null
            val close = bar.close ?: return@mapNotNull null
            val volume = bar.volume ?: 0L
            
            val instant = try {
                Instant.parse(timestamp)
            } catch (e: Exception) {
                return@mapNotNull null
            }
            
            val date = instant.atZone(ZoneOffset.UTC).toLocalDate()
            
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
