package com.talhadev.zamankumandasi.worker

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.talhadev.zamankumandasi.data.repository.AppUsageRepository
import com.talhadev.zamankumandasi.data.repository.AuthRepository
import com.talhadev.zamankumandasi.ui.BlockerActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class AppLimitCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val appUsageRepository: AppUsageRepository
    private val authRepository: AuthRepository

    init {
        val appContext = context.applicationContext
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WorkerEntryPoint::class.java
        )
        appUsageRepository = hiltEntryPoint.appUsageRepository()
        authRepository = hiltEntryPoint.authRepository()
    }

    override suspend fun doWork(): Result {
        return try {
            val currentUser = authRepository.getCurrentUser()
            
            // Sadece çocuk hesapları için kontrol et
            if (currentUser?.userType == com.talhadev.zamankumandasi.data.model.UserType.CHILD) {
                val userApps = appUsageRepository.getAppUsageByUser(currentUser.id)
                
                userApps.forEach { appUsage ->
                    if (appUsage.dailyLimit > 0 && appUsage.usedTime >= appUsage.dailyLimit) {
                        // Limit aşıldı, uygulamayı engelle
                        blockApp(appUsage.packageName, appUsage.appName)
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun blockApp(packageName: String, appName: String) {
        try {
            // BlockerActivity'yi başlat
            val intent = Intent(applicationContext, BlockerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                         Intent.FLAG_ACTIVITY_CLEAR_TOP or
                         Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(BlockerActivity.EXTRA_PACKAGE, packageName)
                putExtra(BlockerActivity.EXTRA_APP_NAME, appName)
                putExtra(BlockerActivity.EXTRA_REASON, "Ebeveynin belirlediği günlük süre sınırı aşıldı")
            }
            applicationContext.startActivity(intent)
            
            // Ana ekrana dön
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            applicationContext.startActivity(homeIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val WORK_NAME = "app_limit_check_worker"
        
        fun startPeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            
            val request = PeriodicWorkRequestBuilder<AppLimitCheckWorker>(
                15, TimeUnit.MINUTES, // 15 dakikada bir çalış
                5, TimeUnit.MINUTES   // 5 dakika esneklik
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
        }
        
        fun stopWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

// Hilt entry point for Worker
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WorkerEntryPoint {
    fun appUsageRepository(): AppUsageRepository
    fun authRepository(): AuthRepository
}
