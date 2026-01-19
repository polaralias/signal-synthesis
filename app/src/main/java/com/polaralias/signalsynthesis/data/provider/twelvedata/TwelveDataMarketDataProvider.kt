package com.polaralias.signalsynthesis.data.provider.twelvedata

import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.provider.DailyProvider
import com.polaralias.signalsynthesis.domain.provider.IntradayProvider
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TwelveDataMarketDataProvider(
    private val apiKey: String,
    private val service: TwelveDataService = TwelveDataService.create(),
    private val clock: Clock = Clock.systemUTC()
) : QuoteProvider, IntradayProvider, DailyProvider {

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        if (symbols.isEmpty()) return emptyMap()
        // Twelve Data free tier usually processes one symbol at a time for the quote endpoint
        // unless using complex batching. We'll iterate for simplicity/safety.
        return symbols.mapNotNull { symbol ->
            try {
                val q = service.getQuote(symbol, apiKey)
                q.toQuote()
            } catch (e: Exception) {
                null
            }
        }.associateBy { it.symbol }
    }

    override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        return try {
            // outputsize is approximate here, e.g. 2 days * 78 bars/day (for 5min)
            val response = service.getTimeSeries(symbol, "5min", days * 100, apiKey)
            response.values.toIntradayBars()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        return try {
            val response = service.getTimeSeries(symbol, "1day", days, apiKey)
            response.values.toDailyBars()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun TwelveDataQuote.toQuote(): Quote? {
        val sym = symbol ?: return null
        val p = price ?: return null
        val vol = volume ?: 0L
        val ts = timestamp?.let { Instant.ofEpochSecond(it) } ?: Instant.now(clock)
        
        return Quote(
            symbol = sym,
            price = p,
            volume = vol,
            timestamp = ts
        )
    }

    private fun List<TwelveDataBar>.toIntradayBars(): List<IntradayBar> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return mapNotNull { bar ->
            val dateStr = bar.datetime ?: return@mapNotNull null
            val time = try {
                LocalDateTime.parse(dateStr, formatter).atZone(ZoneOffset.UTC).toInstant()
            } catch (e: DateTimeParseException) {
                return@mapNotNull null
            }
            IntradayBar(
                time = time,
                open = bar.open ?: 0.0,
                high = bar.high ?: 0.0,
                low = bar.low ?: 0.0,
                close = bar.close ?: 0.0,
                volume = bar.volume ?: 0L
            )
        }
    }

    private fun List<TwelveDataBar>.toDailyBars(): List<DailyBar> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return mapNotNull { bar ->
            val dateStr = bar.datetime ?: return@mapNotNull null
            val date = try {
                if (dateStr.contains(" ")) {
                    LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate()
                } else {
                    LocalDate.parse(dateStr, formatter)
                }
            } catch (e: DateTimeParseException) {
                return@mapNotNull null
            }
            DailyBar(
                date = date,
                open = bar.open ?: 0.0,
                high = bar.high ?: 0.0,
                low = bar.low ?: 0.0,
                close = bar.close ?: 0.0,
                volume = bar.volume ?: 0L
            )
        }
    }
}
