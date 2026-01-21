package com.polaralias.signalsynthesis.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.polaralias.signalsynthesis.R
import com.polaralias.signalsynthesis.domain.model.TradingIntent

import android.app.PendingIntent
import androidx.core.app.TaskStackBuilder
import android.content.Intent
import com.polaralias.signalsynthesis.MainActivity

object NotificationHelper {
    private const val SYSTEM_CHANNEL_ID = "system_alerts_channel"
    private const val MARKET_CHANNEL_ID = "market_alerts_channel"
    
    private const val BLACKLIST_NOTIFICATION_ID = 1001

    fun showBlacklistNotification(context: Context, providerName: String) {
        ensureChannels(context)
        
        val notification = NotificationCompat.Builder(context, SYSTEM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Provider Blocked")
            .setContentText("$providerName has been temporarily paused due to authentication errors.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent(context, null))
            .build()

        notify(context, BLACKLIST_NOTIFICATION_ID, notification)
    }

    fun showMarketAlert(context: Context, symbol: String, title: String, message: String) {
        ensureChannels(context)
        
        val notificationId = (symbol + title).hashCode()
        val notification = NotificationCompat.Builder(context, MARKET_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent(context, symbol))
            .build()

        notify(context, notificationId, notification)
    }

    fun showTradeSignal(context: Context, symbol: String, setupType: String, confidence: Double, intent: TradingIntent) {
        ensureChannels(context)
        
        val intentLabel = when (intent) {
            TradingIntent.LONG_TERM -> "Investment"
            TradingIntent.SWING -> "Swing Trade"
            TradingIntent.DAY_TRADE -> "Day Trade"
        }
        
        val confidencePercent = (confidence * 100).toInt()
        val notificationId = symbol.hashCode()
        val notification = NotificationCompat.Builder(context, MARKET_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New $intentLabel: $symbol")
            .setContentText("$setupType detected with $confidencePercent% confidence.")
            .setPriority(NotificationCompat.PRIORITY_MAX) 
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent(context, symbol))
            .build()

        notify(context, notificationId, notification)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Logger.e("NotificationHelper", "Permission missing for notification", e)
        }
    }

    private fun buildPendingIntent(context: Context, symbol: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            if (symbol != null) {
                putExtra(MainActivity.EXTRA_SYMBOL, symbol)
            }
        }
        
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(
                symbol?.hashCode() ?: 0,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )!!
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // System Channel
            val systemName = "System Alerts"
            val systemDesc = "Notifications for system events like API errors."
            val systemChannel = NotificationChannel(SYSTEM_CHANNEL_ID, systemName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = systemDesc
            }
            notificationManager.createNotificationChannel(systemChannel)

            // Market Channel
            val marketName = "Market Signals"
            val marketDesc = "Notifications for trade signals and price alerts."
            val marketChannel = NotificationChannel(MARKET_CHANNEL_ID, marketName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = marketDesc
            }
            notificationManager.createNotificationChannel(marketChannel)
        }
    }
}
