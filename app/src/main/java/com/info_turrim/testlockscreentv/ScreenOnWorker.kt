package com.info_turrim.testlockscreentv

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenOnWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyApp:MyWakelockTag"
            )
            wakeLock.acquire()
        } catch (e: Exception) {
            Log.d("ScreenOnWorker", "$e")
            Result.failure()
        }

        Result.success()
    }
}