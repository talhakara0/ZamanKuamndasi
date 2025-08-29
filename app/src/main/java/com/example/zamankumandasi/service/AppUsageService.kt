package com.example.zamankumandasi.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.zamankumandasi.R
import com.example.zamankumandasi.data.repository.AppUsageRepository
import com.example.zamankumandasi.data.repository.AuthRepository
import com.example.zamankumandasi.ui.BlockerActivity
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
    private const val TRACKING_INTERVAL = 15000L // 15 saniye
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
                enforceLimits()
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
            val startTime = endTime - (24 * 60 * 60 * 1000) // Son 24 saat (günlük toplam için)

            // Günlük kullanım istatistiklerini al
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val currentUser = authRepository.getCurrentUser()
            currentUser?.let { user ->
                usageStats.forEach { stats ->
                    if (stats.totalTimeInForeground > 60000) { // Minimum 1 dakika kullanım
                        // Uygulama adını al
                        val packageManager = packageManager
                        val appName = try {
                            packageManager.getApplicationLabel(
                                packageManager.getApplicationInfo(stats.packageName, 0)
                            ).toString()
                        } catch (e: Exception) {
                            stats.packageName
                        }
                        
                        // Mevcut veriyi kontrol et ve sadece daha yüksek kullanım varsa güncelle
                        val existingUsage = appUsageRepository.getAppUsageByPackage(user.id, stats.packageName)
                        val shouldUpdate = if (existingUsage != null) {
                            // Sadece günlük kullanım artmışsa güncelle
                            stats.totalTimeInForeground > existingUsage.usedTime
                        } else {
                            true
                        }
                        
                        if (shouldUpdate) {
                            appUsageRepository.updateUsedTime(
                                user.id, 
                                stats.packageName, 
                                if (existingUsage != null) {
                                    stats.totalTimeInForeground - existingUsage.usedTime // Sadece farkı ekle
                                } else {
                                    stats.totalTimeInForeground // İlk kez kayıt
                                }
                            )
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            // Hata durumunda loglama yapılabilir
            e.printStackTrace()
        }
    }

    private suspend fun enforceLimits() {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 60_000 // son 1 dakika olayları yeterli

            val events = usageStatsManager.queryEvents(startTime, endTime)
            var lastForegroundPackage: String? = null
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPackage = event.packageName
                }
            }

            val currentPackage = lastForegroundPackage ?: return

            val user = authRepository.getCurrentUser() ?: return
            val usage = appUsageRepository.getAppUsageByPackage(user.id, currentPackage) ?: return

            // Ebeveyn tarafından belirlenen limit kontrolü - sadece çocuk hesapları için
            if (user.userType == com.example.zamankumandasi.data.model.UserType.CHILD) {
                // Ebeveyn limitlerini kontrol et
                if (usage.dailyLimit > 0 && usage.usedTime >= usage.dailyLimit && !isOurPackage(currentPackage)) {
                    launchBlocker(currentPackage, usage.appName, "Ebeveynin belirlediği günlük süre sınırı aşıldı")
                }
            } else {
                // Ebeveyn hesapları için normal limit kontrolü
                if (usage.dailyLimit > 0 && usage.usedTime >= usage.dailyLimit && !isOurPackage(currentPackage)) {
                    launchBlocker(currentPackage, usage.appName, "Günlük süre sınırı aşıldı")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isOurPackage(pkg: String): Boolean = pkg == packageName

    private fun launchBlocker(packageName: String, appName: String, reason: String = "Günlük süre sınırı aşıldı") {
        val intent = Intent(this, BlockerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockerActivity.EXTRA_PACKAGE, packageName)
            putExtra(BlockerActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockerActivity.EXTRA_REASON, reason)
        }
        startActivity(intent)
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
