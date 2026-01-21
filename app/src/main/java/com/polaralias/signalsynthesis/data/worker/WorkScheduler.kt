package com.polaralias.signalsynthesis.data.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.polaralias.signalsynthesis.data.alerts.MarketAlertWorker
import java.util.concurrent.TimeUnit

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy

interface WorkScheduler {
    fun scheduleAlerts(enabled: Boolean, intervalMinutes: Int = 15)
}

class WorkManagerScheduler(
    private val workManager: WorkManager
) : WorkScheduler {
    override fun scheduleAlerts(enabled: Boolean, intervalMinutes: Int) {
        // Always cancel existing work first
        workManager.cancelUniqueWork(MarketAlertWorker.WORK_NAME)
        
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            if (intervalMinutes >= 15) {
                // Periodic work (Standard)
                val request = PeriodicWorkRequestBuilder<MarketAlertWorker>(
                    intervalMinutes.toLong(), 
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .addTag(MarketAlertWorker.WORK_TAG)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    MarketAlertWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } else {
                // High frequency mode: use OneTimeWorkRequest that reschedules itself
                val data = Data.Builder()
                    .putInt("intervalMinutes", intervalMinutes)
                    .putBoolean("isHighFreq", true)
                    .build()
                
                val request = OneTimeWorkRequestBuilder<MarketAlertWorker>()
                    .setConstraints(constraints)
                    .setInputData(data)
                    .setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
                    .addTag(MarketAlertWorker.WORK_TAG)
                    .build()
                
                workManager.enqueueUniqueWork(
                    MarketAlertWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}
