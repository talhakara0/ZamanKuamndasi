package com.talhadev.zamankumandasi.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object PermissionHelper {
    
    // Kullanım istatistikleri izni kontrolü
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    // Overlay izni kontrolü (Erişilebilirlik servisi yerine)
    fun hasOverlayPermission(context: Context): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val result = Settings.canDrawOverlays(context)
            Log.d("PermissionHelper", "Overlay izni kontrolü: $result (Android ${Build.VERSION.SDK_INT})")
            result
        } else {
            Log.d("PermissionHelper", "Overlay izni kontrolü: true (Android ${Build.VERSION.SDK_INT} - gerekmez)")
            true // Android 6.0 altında izin gerekmez
        }
        
        return hasPermission
    }
    
    // Battery optimizasyonu kontrolü
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    // Accessibility Service kontrolü
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityEnabled = try {
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            accessibilityEnabled == 1
        } catch (e: Exception) {
            false
        }
        
        if (!accessibilityEnabled) return false
        
        val serviceName = "${context.packageName}/com.talhadev.zamankumandasi.service.UltimateBlockerAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        return enabledServices?.contains(serviceName) == true
    }

    // Tüm gerekli izinler kontrolü
    fun checkAllRequiredPermissions(context: Context): PermissionStatus {
        val usageStats = hasUsageStatsPermission(context)
        val overlay = hasOverlayPermission(context) // Overlay izni gerçek kontrol
        val accessibility = isAccessibilityServiceEnabled(context)
        val batteryOptimization = isIgnoringBatteryOptimizations(context)
        
        Log.d("PermissionHelper", "Permission check - Usage: $usageStats, Overlay: $overlay, Accessibility: $accessibility, Battery: $batteryOptimization")
        
        // Temel izinler (Usage Stats, Overlay, Battery) - Accessibility opsiyonel
        val essentialPermissions = usageStats && overlay && batteryOptimization
        val allGranted = essentialPermissions // Accessibility'yi zorunlu kılmayın
        Log.d("PermissionHelper", "Essential permissions granted: $essentialPermissions (Accessibility optional: $accessibility)")
        
        return PermissionStatus(
            usageStats = usageStats,
            overlay = overlay, // Overlay izni gerçek kontrol
            accessibility = accessibility, // Gerçek accessibility service kontrolü
            batteryOptimization = batteryOptimization,
            allGranted = allGranted // Sadece temel izinler zorunlu
        )
    }
    
    // Kullanım istatistikleri ayarlarına yönlendirme
    fun openUsageStatsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionHelper", "Usage stats settings açılamadı: ${e.message}")
            // Fallback: Genel ayarlara yönlendir
            openGeneralSettings(context)
        }
    }
    
    // Overlay izni ayarlarına yönlendirme
    fun openOverlaySettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } else {
                // Android 6.0 altında izin gerekmez
                android.widget.Toast.makeText(
                    context,
                    "Bu Android sürümünde overlay izni gerekmez",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e("PermissionHelper", "Overlay settings açılamadı: ${e.message}")
            try {
                // Alternatif: Uygulama ayarlarına git
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                android.widget.Toast.makeText(
                    context,
                    "Uygulama ayarlarından 'Diğer uygulamalar üzerinde görüntüle' iznini verin",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (e2: Exception) {
                Log.e("PermissionHelper", "App settings de açılamadı: ${e2.message}")
                openGeneralSettings(context)
            }
        }
    }
    
    // Accessibility Service ayarlarına yönlendirme
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionHelper", "Accessibility settings açılamadı: ${e.message}")
            openGeneralSettings(context)
        }
    }
    
    // Battery optimizasyonu ayarlarına yönlendirme
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionHelper", "Battery optimization settings açılamadı: ${e.message}")
            // Fallback: Genel ayarlara yönlendir
            openGeneralSettings(context)
        }
    }
    
    // Genel ayarlara yönlendirme (fallback)
    private fun openGeneralSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionHelper", "General settings açılamadı: ${e.message}")
        }
    }
    
    // İzin durumu data class'ı
    data class PermissionStatus(
        val usageStats: Boolean,
        val overlay: Boolean,
        val accessibility: Boolean,
        val batteryOptimization: Boolean,
        val allGranted: Boolean
    )
}
