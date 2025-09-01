package com.talhadev.zamankumandasi.service

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
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.data.repository.AppUsageRepository
import com.talhadev.zamankumandasi.data.repository.AuthRepository
import com.talhadev.zamankumandasi.ui.BlockerActivity
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
    private const val TRACKING_INTERVAL = 2000L // 2 saniye - çok daha sık kontrol
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
        
        android.util.Log.d("AppUsageService", "🚀 USAGE TRACKING BAŞLADI!")
        serviceScope.launch {
            while (isTracking) {
                try {
                    android.util.Log.d("AppUsageService", "🔄 Tracking döngüsü çalışıyor...")
                    trackAppUsage()
                    enforceLimits()
                    delay(TRACKING_INTERVAL)
                } catch (e: Exception) {
                    android.util.Log.e("AppUsageService", "❌ Tracking hatası: ${e.message}")
                    e.printStackTrace()
                    delay(5000) // Hata durumunda 5 saniye bekle
                }
            }
        }
        android.util.Log.d("AppUsageService", "🔚 USAGE TRACKING DÖNGÜSÜ BİTTİ!")
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
                            
                            // Çocuk hesabı için anında limit kontrolü
                            if (user.userType == com.talhadev.zamankumandasi.data.model.UserType.CHILD) {
                                val updatedUsage = appUsageRepository.getAppUsageByPackage(user.id, stats.packageName)
                                if (updatedUsage != null && 
                                    updatedUsage.dailyLimit > 0 && 
                                    updatedUsage.usedTime >= updatedUsage.dailyLimit) {
                                    // Limit aşıldı, hemen engelle
                                    forceGoHome()
                                    launchBlocker(stats.packageName, appName, "Ebeveynin belirlediği günlük süre sınırı aşıldı")
                                }
                            }
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
            android.util.Log.d("AppUsageService", "🔍 ENFORCE LIMITS BAŞLADI")
            
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5_000 // son 5 saniye - çok kısa zaman aralığı

            val events = usageStatsManager.queryEvents(startTime, endTime)
            var lastForegroundPackage: String? = null
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPackage = event.packageName
                    android.util.Log.d("AppUsageService", "📱 Tespit edilen uygulama: $lastForegroundPackage")
                }
            }

            val currentPackage = lastForegroundPackage ?: run {
                android.util.Log.d("AppUsageService", "❌ Hiçbir uygulama tespit edilmedi")
                return
            }

            android.util.Log.d("AppUsageService", "🎯 Kontrol edilen uygulama: $currentPackage")

            val user = authRepository.getCurrentUser() ?: run {
                android.util.Log.d("AppUsageService", "❌ Kullanıcı bulunamadı")
                return
            }
            
            android.util.Log.d("AppUsageService", "👤 Kullanıcı tipi: ${user.userType}")
            
            val usage = appUsageRepository.getAppUsageByPackage(user.id, currentPackage) ?: run {
                android.util.Log.d("AppUsageService", "❌ Uygulama usage bilgisi bulunamadı: $currentPackage")
                return
            }
            
            android.util.Log.d("AppUsageService", "📊 Uygulama: ${usage.appName}, Limit: ${usage.dailyLimit}, Kullanılan: ${usage.usedTime}")

            // Ebeveyn tarafından belirlenen limit kontrolü - sadece çocuk hesapları için
            if (user.userType == com.talhadev.zamankumandasi.data.model.UserType.CHILD) {
                android.util.Log.d("AppUsageService", "🧒 ÇOCUK HESABI tespit edildi")
                
                val isOurApp = isOurPackage(currentPackage)
                android.util.Log.d("AppUsageService", "🏠 Kendi uygulamamız mı? $isOurApp")
                
                val hasLimit = usage.dailyLimit > 0
                val limitExceeded = usage.usedTime >= usage.dailyLimit
                
                android.util.Log.d("AppUsageService", "⏰ Limit var mı? $hasLimit")
                android.util.Log.d("AppUsageService", "🚫 Limit aşıldı mı? $limitExceeded (${usage.usedTime} >= ${usage.dailyLimit})")
                
                // Ebeveyn limitlerini kontrol et
                if (usage.dailyLimit > 0 && usage.usedTime >= usage.dailyLimit && !isOurPackage(currentPackage)) {
                    
                    android.util.Log.d("AppUsageService", "🚨 LİMİT AŞILDI - HEMEN ENGELLE: $currentPackage")
                    android.util.Log.d("AppUsageService", "💥 ENGELLEME BAŞLIYOR...")
                    
                    // NÜKLEER UYGULAMA KİLL SİSTEMİ
                    android.util.Log.d("AppUsageService", "💥 NÜKLEER UYGULAMA KİLL SİSTEMİ BAŞLIYOR!")
                    
                    // 1. Önce uygulamayı KİLL et
                    killApp(currentPackage)
                    
                    // 2. Sonra 10 kez ana ekrana dön
                    repeat(10) {
                        android.util.Log.d("AppUsageService", "🏠 Ana ekrana dönüyor... (${it + 1}/10)")
                        forceGoHome()
                        delay(200) // 0.2 saniye bekle - daha hızlı
                    }
                    
                    // 3. Tekrar uygulamayı KİLL et
                    delay(1000)
                    killApp(currentPackage)
                    
                    android.util.Log.d("AppUsageService", "🛡️ Blocker başlatılıyor...")
                    launchBlocker(currentPackage, usage.appName, "Ebeveynin belirlediği günlük süre sınırı aşıldı")
                    
                    // Blocker'dan sonra tekrar ana ekrana dön
                    delay(1000)
                    android.util.Log.d("AppUsageService", "🏠 Son ana ekrana dönüş...")
                    forceGoHome()
                } else {
                    android.util.Log.d("AppUsageService", "✅ Uygulama engellenmiyor - şartlar sağlanmıyor")
                    if (usage.dailyLimit <= 0) android.util.Log.d("AppUsageService", "   - Limit belirlenmemiş")
                    if (usage.usedTime < usage.dailyLimit) android.util.Log.d("AppUsageService", "   - Limit henüz aşılmamış")
                    if (isOurPackage(currentPackage)) android.util.Log.d("AppUsageService", "   - Kendi uygulamamız")
                }
            } else {
                android.util.Log.d("AppUsageService", "👨‍👩‍👧‍👦 EBEVEYN HESABI tespit edildi")
                // Ebeveyn hesapları için normal limit kontrolü
                if (usage.dailyLimit > 0 && usage.usedTime >= usage.dailyLimit && !isOurPackage(currentPackage)) {
                    forceGoHome()
                    launchBlocker(currentPackage, usage.appName, "Günlük süre sınırı aşıldı")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forceGoHome() {
        try {
            android.util.Log.d("AppUsageService", "💪 GÜÇLÜ ANA EKRANA DÖNÜŞ BAŞLADI")
            
            // 1. Önce normal ana ekrana dön
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            startActivity(homeIntent)
            
            // 2. Hemen ardından tekrar ana ekrana dön (çifte vuruş)
            Thread.sleep(100)
            startActivity(homeIntent)
            
            // 3. Son olarak tekrar ana ekrana dön (üçlü vuruş)
            Thread.sleep(100)
            startActivity(homeIntent)
            
            android.util.Log.d("AppUsageService", "💪 GÜÇLÜ ANA EKRANA DÖNÜŞ TAMAMLANDI")
            
        } catch (e: Exception) {
            android.util.Log.e("AppUsageService", "❌ Ana ekrana dönüş hatası: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun isOurPackage(pkg: String): Boolean = pkg == packageName

    private fun killApp(packageName: String) {
        try {
            android.util.Log.d("AppUsageService", "💀 NÜKLEER UYGULAMA KİLL BAŞLIYOR: $packageName")
            
            // 1. ActivityManager ile uygulamayı kapat
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(packageName)
            
            // 2. Tekrar kill background processes
            am.killBackgroundProcesses(packageName)
            
            // 3. Hemen tekrar kill
            Thread.sleep(200)
            am.killBackgroundProcesses(packageName)
            
            // 4. Son kez kill
            Thread.sleep(200)
            am.killBackgroundProcesses(packageName)
            
            android.util.Log.d("AppUsageService", "✅ NÜKLEER UYGULAMA KİLL TAMAMLANDI: $packageName")
            
        } catch (e: Exception) {
            android.util.Log.e("AppUsageService", "❌ Uygulama kill hatası: ${e.message}")
            e.printStackTrace()
        }
    }

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
