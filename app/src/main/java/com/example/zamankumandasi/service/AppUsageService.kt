package com.example.zamankumandasi.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.zamankumandasi.R
import com.example.zamankumandasi.data.repository.AppUsageRepository
import com.example.zamankumandasi.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AppUsageService : Service() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository
    
    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isTracking = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "app_usage_service"
        private const val TRACKING_INTERVAL = 30000L // 30 saniye
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_TRACKING" -> startTracking()
            "STOP_TRACKING" -> stopTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        if (isTracking) return
        
        isTracking = true
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            while (isTracking) {
                trackAppUsage()
                delay(TRACKING_INTERVAL)
            }
        }
    }

    private fun stopTracking() {
        isTracking = false
        stopForeground(true)
        stopSelf()
    }

    private suspend fun trackAppUsage() {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TRACKING_INTERVAL

            // Daha basit yaklaşım: Günlük kullanım istatistiklerini al
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val currentUser = authRepository.getCurrentUser()
            currentUser?.let { user ->
                usageStats.forEach { stats ->
                    if (stats.totalTimeInForeground > 0) {
                        // Uygulama adını al
                        val packageManager = packageManager
                        val appName = try {
                            packageManager.getApplicationLabel(
                                packageManager.getApplicationInfo(stats.packageName, 0)
                            ).toString()
                        } catch (e: Exception) {
                            stats.packageName
                        }
                        
                        // Firestore'a kaydet
                        appUsageRepository.updateUsedTime(
                            user.id, 
                            stats.packageName, 
                            stats.totalTimeInForeground
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            // Hata durumunda loglama yapılabilir
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Uygulama Kullanım Takibi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Uygulama kullanım verilerini takip eder"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zaman Kumandası")
            .setContentText("Uygulama kullanımı takip ediliyor")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
