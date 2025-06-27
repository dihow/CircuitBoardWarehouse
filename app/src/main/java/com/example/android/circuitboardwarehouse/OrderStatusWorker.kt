package com.example.android.circuitboardwarehouse

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class OrderStatusWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val checker = OrderStatusChecker()
        return try {
            checker.checkOrdersStatus()
            Result.success()
        } catch (e: Exception) {
            Log.e("OrderStatusWorker", "Error checking order status", e)
            Result.failure()
        }
    }
}
