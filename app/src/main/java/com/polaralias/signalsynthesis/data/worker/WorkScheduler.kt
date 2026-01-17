package com.polaralias.signalsynthesis.data.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.polaralias.signalsynthesis.data.alerts.MarketAlertWorker
import java.util.concurrent.TimeUnit

interface WorkScheduler {
    fun scheduleAlerts(enabled: Boolean)
}

class WorkManagerScheduler(
    private val workManager: WorkManager
) : WorkScheduler {
    override fun scheduleAlerts(enabled: Boolean) {
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<MarketAlertWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(MarketAlertWorker.WORK_TAG)
                .build()
            workManager.enqueueUniquePeriodicWork(
                MarketAlertWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(MarketAlertWorker.WORK_NAME)
        }
    }
}
