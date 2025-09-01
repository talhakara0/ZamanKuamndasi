package com.talhadev.zamankumandasi.service

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.talhadev.zamankumandasi.data.repository.AppUsageRepository
import com.talhadev.zamankumandasi.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AntiBypassService : Service() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository
    
    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastBlockedApp: String? = null
    private var blockCount = 0

    companion object {
        private const val ULTRA_FAST_CHECK = 200L // 0.2 saniye - NÜKLEER HIZ
    }

    override fun onCreate() {
        super.onCreate()
        startUltraFastCheck()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startUltraFastCheck() {
        serviceScope.launch {
            while (true) {
                try {
                    checkAndBlockInstantly()
                    delay(ULTRA_FAST_CHECK)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(1000) // Hata durumunda 1 saniye bekle
                }
            }
        }
    }

    private suspend fun checkAndBlockInstantly() {
        try {
            val currentUser = authRepository.getCurrentUser()
            
            // Sadece çocuk hesapları için kontrol et
            if (currentUser?.userType != com.talhadev.zamankumandasi.data.model.UserType.CHILD) {
                return
            }

            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 500 // Son 0.5 saniye - NÜKLEER HIZ

            val events = usageStatsManager.queryEvents(startTime, endTime)
            var currentForegroundApp: String? = null
            val event = android.app.usage.UsageEvents.Event()
            
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    currentForegroundApp = event.packageName
                }
            }

            val currentPackage = currentForegroundApp ?: return

            // Kendi uygulamamızı engelleme
            if (currentPackage == packageName) {
                return
            }

            // Uygulama limitini kontrol et
            val usage = appUsageRepository.getAppUsageByPackage(currentUser.id, currentPackage)

            if (usage != null && 
                usage.dailyLimit > 0 && 
                usage.usedTime >= usage.dailyLimit) {
                
                // Aynı uygulamayı engelleme sayısını artır
                if (lastBlockedApp == currentPackage) {
                    blockCount++
                } else {
                    lastBlockedApp = currentPackage
                    blockCount = 1
                }
                
                android.util.Log.d("AntiBypass", "ANINDA ENGELLE: $currentPackage (${blockCount}. kez)")
                
                // NÜKLEER SEVİYE ENGELLEME
                android.util.Log.d("AntiBypass", "NÜKLEER ENGELLEME BAŞLADI: $currentPackage")
                
                // 10 kez ana ekrana dön - NÜKLEER SEVİYE
                repeat(10) {
                    forceHomeInstantly()
                    delay(50) // 0.05 saniye - ÇOK HIZLI
                }
                
                // Israrlı kullanıcılar için EKSTRA NÜKLEER ÖNLEM
                if (blockCount > 2) {
                    android.util.Log.d("AntiBypass", "NÜKLEER ISRARLI KULLANICI TESPİT EDİLDİ!")
                    
                    // 20 kez ana ekrana dön
                    repeat(20) {
                        forceHomeInstantly()
                        delay(25) // 0.025 saniye - SÜPER HIZLI
                    }
                    
                    // 5 saniye beklet
                    delay(5000)
                    
                    // Tekrar 10 kez ana ekrana dön
                    repeat(10) {
                        forceHomeInstantly()
                        delay(50)
                    }
                }
            } else {
                // Uygulama legal - block count sıfırla
                if (lastBlockedApp != currentPackage) {
                    lastBlockedApp = null
                    blockCount = 0
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forceHomeInstantly() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
