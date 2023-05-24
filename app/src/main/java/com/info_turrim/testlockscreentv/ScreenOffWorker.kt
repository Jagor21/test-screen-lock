package com.info_turrim.testlockscreentv

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenOffWorker(
    private val context: Context,
    params: WorkerParameters)
//    : CoroutineWorker(context, params) {
//
//    override suspend fun doWork(): Result= withContext(Dispatchers.IO) {
////        try {
////            val devicePolicyManager =
////                getSystemService(context, DevicePolicyManager::class.java) as DevicePolicyManager
////            devicePolicyManager.lockNow()
////        } catch (e: Exception) {
////            Result.failure()
////        }
////
////        Result.success()
//    }
//}