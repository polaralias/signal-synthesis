package com.polaralias.signalsynthesis.data.provider.twelvedata

import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.provider.DailyProvider
import com.polaralias.signalsynthesis.domain.provider.IntradayProvider
import com.polaralias.signalsynthesis.domain.provider.MetricsProvider
import com.polaralias.signalsynthesis.domain.provider.ProfileProvider
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import com.polaralias.signalsynthesis.util.Logger
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
) : QuoteProvider, IntradayProvider, DailyProvider, ProfileProvider, MetricsProvider {

    override suspend fun getProfile(symbol: String): CompanyProfile? {
        if (symbol.isBlank()) return null
        return try {
            val response = service.getProfile(symbol, apiKey)
            if (response.hasError()) {
                Logger.w("TwelveData", "Profile error for $symbol: ${response.message ?: response.status.orEmpty()}")
                return null
            }
            CompanyProfile(
                name = response.name ?: symbol,
                sector = response.sector,
                industry = response.industry,
                description = response.description
            )
        } catch (e: Exception) {
            Logger.e("TwelveData", "Profile fetch failed for $symbol", e)
            null
        }
    }

    override suspend fun getMetrics(symbol: String): FinancialMetrics? {
        if (symbol.isBlank()) return null
        return try {
            val response = service.getStatistics(symbol, apiKey)
            if (response.hasError()) {
                Logger.w("TwelveData", "Metrics error for $symbol: ${response.message ?: response.status.orEmpty()}")
                return null
            }
            val valuations = response.valuations_metrics
            val dividends = response.dividends_and_splits
            FinancialMetrics(
                marketCap = valuations?.marketCapRaw.toLongSafe(),
                peRatio = valuations?.peRatioRaw.toDoubleSafe(),
                eps = null,
                pbRatio = valuations?.pbRatioRaw.toDoubleSafe(),
                dividendYield = dividends?.dividendYieldRaw.toDoubleSafe()
            )
        } catch (e: Exception) {
            Logger.e("TwelveData", "Metrics fetch failed for $symbol", e)
            null
        }
    }

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        if (symbols.isEmpty()) return emptyMap()
        // Twelve Data free tier usually processes one symbol at a time for the quote endpoint
        // unless using complex batching. We'll iterate for simplicity/safety.
        return symbols.mapNotNull { symbol ->
            try {
                val q = service.getQuote(symbol, apiKey)
                if (q.hasError()) {
                    Logger.w("TwelveData", "Quote error for $symbol: ${q.message ?: q.status.orEmpty()}")
                    null
                } else {
                    q.toQuote()
                }
            } catch (e: Exception) {
                Logger.e("TwelveData", "Quote fetch failed for $symbol", e)
                null
            }
        }.associateBy { it.symbol }
    }

    override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        // outputsize is approximate here, e.g. 2 days * 78 bars/day (for 5min)
        val response = service.getTimeSeries(symbol, "5min", days * 100, apiKey)
        if (response.hasError()) {
            Logger.w("TwelveData", "Intraday error for $symbol: ${response.message ?: response.status.orEmpty()}")
            return emptyList()
        }
        return response.values.orEmpty().toIntradayBars()
    }

    override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        val response = service.getTimeSeries(symbol, "1day", days, apiKey)
        if (response.hasError()) {
            Logger.w("TwelveData", "Daily error for $symbol: ${response.message ?: response.status.orEmpty()}")
            return emptyList()
        }
        return response.values.orEmpty().toDailyBars()
    }

    private fun TwelveDataQuote.toQuote(): Quote? {
        val sym = symbol ?: return null
        val p = priceRaw.toDoubleSafe() ?: return null
        val vol = volumeRaw.toLongSafe() ?: 0L
        val ts = timestampRaw.toEpochInstant() ?: Instant.now(clock)
        
        return Quote(
            symbol = sym,
            price = p,
            volume = vol,
            timestamp = ts,
            changePercent = percentChangeRaw.toDoubleSafe()
        )
    }

    private fun List<TwelveDataBar>.toIntradayBars(): List<IntradayBar> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return mapNotNull { bar ->
            val dateStr = bar.datetime ?: return@mapNotNull null
            val time = try {
                LocalDateTime.parse(dateStr, formatter).atZone(ZoneOffset.UTC).toInstant()
            } catch (e: DateTimeParseException) {
                Logger.w("TwelveData", "Intraday parse failed: $dateStr", e)
                return@mapNotNull null
            }
            IntradayBar(
                time = time,
                open = bar.open.toDoubleSafe() ?: 0.0,
                high = bar.high.toDoubleSafe() ?: 0.0,
                low = bar.low.toDoubleSafe() ?: 0.0,
                close = bar.close.toDoubleSafe() ?: 0.0,
                volume = bar.volume.toLongSafe() ?: 0L
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
                Logger.w("TwelveData", "Daily parse failed: $dateStr", e)
                return@mapNotNull null
            }
            DailyBar(
                date = date,
                open = bar.open.toDoubleSafe() ?: 0.0,
                high = bar.high.toDoubleSafe() ?: 0.0,
                low = bar.low.toDoubleSafe() ?: 0.0,
                close = bar.close.toDoubleSafe() ?: 0.0,
                volume = bar.volume.toLongSafe() ?: 0L
            )
        }
    }

    private fun TwelveDataQuote.hasError(): Boolean {
        return status.equals("error", ignoreCase = true) || !message.isNullOrBlank()
    }

    private fun TwelveDataTimeSeries.hasError(): Boolean {
        return status.equals("error", ignoreCase = true) || !message.isNullOrBlank()
    }

    private fun TwelveDataProfile.hasError(): Boolean {
        return status.equals("error", ignoreCase = true) || !message.isNullOrBlank()
    }

    private fun TwelveDataStatistics.hasError(): Boolean {
        return status.equals("error", ignoreCase = true) || !message.isNullOrBlank()
    }

    private fun Any?.toDoubleSafe(): Double? {
        val raw = when (this) {
            null -> return null
            is Number -> this.toDouble().toString()
            else -> this.toString()
        }
        if (raw.isBlank()) return null
        return raw.replace(",", "").toDoubleOrNull()
    }

    private fun Any?.toLongSafe(): Long? {
        val raw = when (this) {
            null -> return null
            is Number -> this.toLong().toString()
            else -> this.toString()
        }
        if (raw.isBlank()) return null
        return raw.replace(",", "").toLongOrNull()
    }

    private fun Any?.toEpochInstant(): Instant? {
        val epoch = toLongSafe() ?: return null
        return if (epoch > 10_000_000_000L) {
            Instant.ofEpochMilli(epoch)
        } else {
            Instant.ofEpochSecond(epoch)
        }
    }
}
