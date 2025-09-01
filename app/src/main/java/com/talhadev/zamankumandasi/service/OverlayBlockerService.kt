package com.talhadev.zamankumandasi.service

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.data.repository.AppUsageRepository
import com.talhadev.zamankumandasi.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class OverlayBlockerService : Service() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository
    
    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var isBlocking = false

    companion object {
        private const val CHECK_INTERVAL = 1000L // 1 saniye - daha sık kontrol
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startContinuousCheck()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startContinuousCheck() {
        serviceScope.launch {
            while (true) {
                try {
                    checkForBlockedApps()
                    delay(CHECK_INTERVAL)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun checkForBlockedApps() {
        try {
            val currentUser = authRepository.getCurrentUser()
            
            // Sadece çocuk hesapları için kontrol et
            if (currentUser?.userType != com.talhadev.zamankumandasi.data.model.UserType.CHILD) {
                hideOverlay()
                return
            }

            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 10_000 // Son 10 saniye - daha kısa zaman

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

            // Kendi uygulamamızı kontrol etme
            if (currentPackage == packageName) {
                hideOverlay()
                return
            }

            val usage = appUsageRepository.getAppUsageByPackage(currentUser.id, currentPackage)

            if (usage != null && 
                usage.dailyLimit > 0 && 
                usage.usedTime >= usage.dailyLimit) {
                
                android.util.Log.d("OverlayBlocker", "Engellenmiş uygulama tespit edildi: $currentPackage")
                
                // HEMEN ana ekrana git - overlay göstermeden önce
                goToHome()
                
                // Sonra overlay göster
                showBlockOverlay(usage.appName, currentPackage)
                
                // 2 saniye sonra tekrar ana ekrana git
                delay(2000)
                goToHome()
                
            } else {
                // Uygulama engellenmemiş, overlay'i gizle
                hideOverlay()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showBlockOverlay(appName: String, packageName: String) {
        if (isBlocking) return

        try {
            hideOverlay() // Önce mevcut overlay'i kaldır

            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_app_blocked, null)
            
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )
            
            layoutParams.gravity = Gravity.CENTER

            // UI elemanlarını ayarla
            overlayView?.findViewById<TextView>(R.id.tvBlockedAppName)?.text = "$appName engellendi"
            overlayView?.findViewById<TextView>(R.id.tvBlockReason)?.text = "Günlük kullanım süreniz doldu"
            
            overlayView?.findViewById<Button>(R.id.btnGoHome)?.setOnClickListener {
                goToHome()
            }

            windowManager?.addView(overlayView, layoutParams)
            isBlocking = true

            // Ana ekrana git
            goToHome()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        try {
            if (overlayView != null && isBlocking) {
                windowManager?.removeView(overlayView)
                overlayView = null
                isBlocking = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun goToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        serviceScope.cancel()
    }
}
