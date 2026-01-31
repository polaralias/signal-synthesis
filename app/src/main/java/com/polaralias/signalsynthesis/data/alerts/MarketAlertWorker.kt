package com.polaralias.signalsynthesis.data.alerts

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.polaralias.signalsynthesis.util.NotificationHelper
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.polaralias.signalsynthesis.MainActivity
import com.polaralias.signalsynthesis.R
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import java.util.concurrent.TimeUnit
import com.polaralias.signalsynthesis.data.provider.ProviderFactory
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.data.storage.AlertSettingsStore
import com.polaralias.signalsynthesis.data.storage.ApiKeyStore
import com.polaralias.signalsynthesis.domain.indicators.RsiIndicator
import com.polaralias.signalsynthesis.domain.indicators.VwapIndicator
import com.polaralias.signalsynthesis.data.alerts.AlertType
import com.polaralias.signalsynthesis.data.alerts.AlertDirection
import kotlin.math.roundToInt

class MarketAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val alertStore = AlertSettingsStore(applicationContext)
        val appSettingsStore = com.polaralias.signalsynthesis.data.storage.AppSettingsStore(applicationContext)
        val settings = alertStore.loadSettings()
        val appSettings = appSettingsStore.loadSettings()
        
        if (!settings.enabled || appSettings.isAnalysisPaused) return Result.success()

        val symbols = alertStore.loadSymbols()
        if (symbols.isEmpty()) return Result.success()

        val apiKeys = ApiKeyStore(applicationContext).loadApiKeys()
        if (!apiKeys.hasAny()) return Result.success()

        val repository = MarketDataRepository(ProviderFactory().build(apiKeys))
        val quotes = repository.getQuotes(symbols)
        if (quotes.isEmpty()) return Result.success()

        val targets = alertStore.loadTargets().associateBy { it.symbol.uppercase() }
        val cooldownMs = settings.cooldownMinutes.toLong() * 60_000L
        val sentThisRun = mutableSetOf<String>()
        suspend fun shouldNotify(symbol: String, type: AlertType): Boolean {
            val key = "${symbol}_${type.name}"
            if (sentThisRun.contains(key)) return false
            val last = alertStore.getLastAlertTimestamp(symbol, type)
            val now = System.currentTimeMillis()
            if (now - last < cooldownMs) return false
            sentThisRun.add(key)
            alertStore.setLastAlertTimestamp(symbol, type, now)
            return true
        }

        val alerts = mutableListOf<AlertEvent>()
        for (symbol in symbols) {
            kotlinx.coroutines.yield()
            if (isStopped) break
            
            val quote = quotes[symbol] ?: continue
            val bars = repository.getIntraday(symbol, 1)
            val vwap = VwapIndicator.calculate(bars)
            val rsi = RsiIndicator.calculateFromIntraday(bars)

            if (vwap != null && quote.price < vwap * (1.0 - settings.vwapDipPercent / 100.0)) {
                if (shouldNotify(symbol, AlertType.VWAP_DIP)) {
                    alerts.add(AlertEvent(symbol, AlertType.VWAP_DIP, "Investment Signal", "Price below VWAP (Value Found)", quote.price, vwap))
                }
            }
            if (rsi != null && rsi <= settings.rsiOversold) {
                if (shouldNotify(symbol, AlertType.RSI_OVERSOLD)) {
                    alerts.add(AlertEvent(symbol, AlertType.RSI_OVERSOLD, "Buy Opportunity", "RSI oversold (${rsi.roundToInt()})", quote.price, vwap))
                }
            }
            if (rsi != null && rsi >= settings.rsiOverbought) {
                if (shouldNotify(symbol, AlertType.RSI_OVERBOUGHT)) {
                    alerts.add(AlertEvent(symbol, AlertType.RSI_OVERBOUGHT, "Sell Warning", "RSI overbought (${rsi.roundToInt()})", quote.price, vwap))
                }
            }

            val target = targets[symbol]
            if (target != null && target.targetPrice > 0.0) {
                val hit = when (target.direction) {
                    AlertDirection.ABOVE -> quote.price >= target.targetPrice
                    AlertDirection.BELOW -> quote.price <= target.targetPrice
                }
                if (hit && shouldNotify(symbol, AlertType.PRICE_TARGET)) {
                    val directionText = if (target.direction == AlertDirection.ABOVE) "above" else "below"
                    alerts.add(
                        AlertEvent(
                            symbol,
                            AlertType.PRICE_TARGET,
                            "Price Target",
                            "Price moved $directionText target ${formatPrice(target.targetPrice)}",
                            quote.price,
                            vwap
                        )
                    )
                }
            }
        }

        if (alerts.isNotEmpty()) {
            for (event in alerts) {
                NotificationHelper.showMarketAlert(
                    context = applicationContext,
                    symbol = event.symbol,
                    title = "${event.category}: ${event.symbol}",
                    message = event.longText
                )
            }
        }

        // Handle high-frequency rescheduling
        val isHighFreq = inputData.getBoolean("isHighFreq", false)
        val intervalMinutes = inputData.getInt("intervalMinutes", 15)
        
        if (isHighFreq && settings.enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val nextRequest = OneTimeWorkRequestBuilder<MarketAlertWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .build()
                
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                nextRequest
            )
        }

        return Result.success()
    }


    data class AlertEvent(
        val symbol: String,
        val type: AlertType,
        val category: String,
        val reason: String,
        val price: Double,
        val vwap: Double?
    ) {
        val longText: String = buildString {
            append("$reason. ")
            append("Current price ")
            append(formatPrice(price))
            if (vwap != null) {
                append(", VWAP reference ")
                append(formatPrice(vwap))
            }
        }
    }

    companion object {
        const val WORK_NAME = "market_alerts"
        const val WORK_TAG = "market_alerts_tag"
    }
}

private fun formatPrice(value: Double): String = "$" + String.format("%.2f", value)
