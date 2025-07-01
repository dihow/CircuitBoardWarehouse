package com.example.android.circuitboardwarehouse

import android.app.Application
import android.app.backup.BackupManager
import android.os.Build
import androidx.work.*
import java.util.concurrent.TimeUnit

class PcbWarehouseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        BackupManager(this).dataChanged()

        WarehouseRepository.initialize(this)
        setupOrderStatusWorker()
    }

    private fun setupOrderStatusWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<OrderStatusWorker>(
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OrderStatusCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}