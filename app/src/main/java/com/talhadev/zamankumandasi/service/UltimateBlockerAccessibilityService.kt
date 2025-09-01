package com.talhadev.zamankumandasi.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.talhadev.zamankumandasi.data.repository.AppUsageRepository
import com.talhadev.zamankumandasi.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class UltimateBlockerAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository
    
    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastBlockedApp: String? = null
    private var blockCount = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        
        serviceInfo = info
        android.util.Log.d("UltimateBlocker", "🚀 ULTIMATE BLOCKER ACCESSIBILITY SERVICE BAŞLADI!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        serviceScope.launch {
            try {
                val packageName = event.packageName?.toString() ?: return@launch
                
                // Kendi uygulamamızı kontrol etme
                if (packageName == this@UltimateBlockerAccessibilityService.packageName) return@launch
                
                android.util.Log.d("UltimateBlocker", "📱 Uygulama tespit edildi: $packageName")
                
                // Kullanıcı kontrolü
                val currentUser = authRepository.getCurrentUser()
                if (currentUser?.userType != com.talhadev.zamankumandasi.data.model.UserType.CHILD) {
                    return@launch
                }
                
                // Uygulama limitini kontrol et
                val usage = appUsageRepository.getAppUsageByPackage(currentUser.id, packageName)
                
                if (usage != null && 
                    usage.dailyLimit > 0 && 
                    usage.usedTime >= usage.dailyLimit) {
                    
                    // Aynı uygulamayı engelleme sayısını artır
                    if (lastBlockedApp == packageName) {
                        blockCount++
                    } else {
                        lastBlockedApp = packageName
                        blockCount = 1
                    }
                    
                    android.util.Log.d("UltimateBlocker", "🚨 LİMİT AŞILDI - ULTIMATE ENGELLEME: $packageName (${blockCount}. kez)")
                    
                    // ULTIMATE ENGELLEME SEQUENCE
                    ultimateBlockSequence(packageName, usage.appName)
                    
                } else {
                    if (lastBlockedApp != packageName) {
                        lastBlockedApp = null
                        blockCount = 0
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("UltimateBlocker", "❌ Accessibility event hatası: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun ultimateBlockSequence(packageName: String, appName: String) {
        try {
            android.util.Log.d("UltimateBlocker", "💥 ULTIMATE BLOCK SEQUENCE BAŞLIYOR!")
            
            // 1. HEMEN ana ekrana git
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            // 2. Recent apps'i temizle
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            Thread.sleep(500)
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            // 3. Tekrar ana ekrana git
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            // 4. BlockerActivity'yi başlat
            val intent = Intent(this, com.talhadev.zamankumandasi.ui.BlockerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("extra_package", packageName)
                putExtra("extra_app_name", appName)
                putExtra("extra_reason", "Ebeveynin belirlediği günlük süre sınırı aşıldı")
            }
            startActivity(intent)
            
            // 5. Son kez ana ekrana git
            Thread.sleep(1000)
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            android.util.Log.d("UltimateBlocker", "✅ ULTIMATE BLOCK SEQUENCE TAMAMLANDI!")
            
        } catch (e: Exception) {
            android.util.Log.e("UltimateBlocker", "❌ Block sequence hatası: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        android.util.Log.d("UltimateBlocker", "⚠️ Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        android.util.Log.d("UltimateBlocker", "🔚 ULTIMATE BLOCKER ACCESSIBILITY SERVICE DURDURULDU!")
    }
}
