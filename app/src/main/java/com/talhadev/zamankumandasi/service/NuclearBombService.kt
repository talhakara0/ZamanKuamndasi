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
class NuclearBombService : Service() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository
    
    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var blockedApps = mutableSetOf<String>()
    private var nuclearMode = false

    companion object {
        private const val NUCLEAR_CHECK = 100L // 0.1 saniye - NÜKLEER BOMB HIZI
    }

    override fun onCreate() {
        super.onCreate()
        startNuclearBomb()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startNuclearBomb() {
        serviceScope.launch {
            while (true) {
                try {
                    nuclearBombCheck()
                    delay(NUCLEAR_CHECK)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(500)
                }
            }
        }
    }

    private suspend fun nuclearBombCheck() {
        try {
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser?.userType != com.talhadev.zamankumandasi.data.model.UserType.CHILD) {
                return
            }

            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 100 // Son 0.1 saniye - NÜKLEER BOMB

            val events = usageStatsManager.queryEvents(startTime, endTime)
            var currentApp: String? = null
            val event = android.app.usage.UsageEvents.Event()
            
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    currentApp = event.packageName
                }
            }

            val currentPackage = currentApp ?: return

            if (currentPackage == packageName) {
                return
            }

            // NÜKLEER BOMB KONTROLÜ
            val usage = appUsageRepository.getAppUsageByPackage(currentUser.id, currentPackage)

            if (usage != null && 
                usage.dailyLimit > 0 && 
                usage.usedTime >= usage.dailyLimit) {
                
                blockedApps.add(currentPackage)
                nuclearMode = true
                
                android.util.Log.d("NuclearBomb", "🚀 NÜKLEER BOMB ATILDI: $currentPackage")
                
                // NÜKLEER BOMB SEQUENCE
                nuclearBombSequence(currentPackage)
                
            } else {
                if (blockedApps.contains(currentPackage)) {
                    blockedApps.remove(currentPackage)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun nuclearBombSequence(packageName: String) {
        try {
            // FASE 1: ANINDA NÜKLEER SALDIRI
            repeat(50) { // 50 kez ana ekrana dön
                nuclearForceHome()
                delay(10) // 0.01 saniye - SÜPER NÜKLEER HIZ
            }
            
            // FASE 2: NÜKLEER BEKLEME
            delay(1000)
            
            // FASE 3: İKİNCİ NÜKLEER DALGA
            repeat(30) {
                nuclearForceHome()
                delay(20)
            }
            
            // FASE 4: NÜKLEER KİLİT
            delay(2000)
            
            // FASE 5: SON NÜKLEER SALDIRI
            repeat(20) {
                nuclearForceHome()
                delay(15)
            }
            
            android.util.Log.d("NuclearBomb", "💥 NÜKLEER BOMB SEQUENCE TAMAMLANDI: $packageName")
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun nuclearForceHome() {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
