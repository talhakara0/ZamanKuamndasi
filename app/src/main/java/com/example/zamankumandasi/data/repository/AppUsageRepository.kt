package com.example.zamankumandasi.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.example.zamankumandasi.data.model.AppInfo
import com.example.zamankumandasi.data.model.AppUsage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    
    suspend fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return installedApps
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(packageManager).toString()
                )
            }
            .sortedBy { it.appName }
    }
    
    suspend fun getAppUsageStats(userId: String, startTime: Long, endTime: Long): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        return usageStats.associate { stats ->
            stats.packageName to stats.totalTimeInForeground
        }
    }
    
    suspend fun saveAppUsage(appUsage: AppUsage) {
        firestore.collection("app_usage").document(appUsage.id).set(appUsage).await()
    }
    
    suspend fun getAppUsageByUser(userId: String): List<AppUsage> {
        val snapshot = firestore.collection("app_usage")
            .whereEqualTo("userId", userId)
            .get().await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(AppUsage::class.java)
        }
    }
    
    suspend fun updateAppUsage(appUsage: AppUsage) {
        firestore.collection("app_usage").document(appUsage.id).set(appUsage).await()
    }
    
    suspend fun getAppUsageByPackage(userId: String, packageName: String): AppUsage? {
        val snapshot = firestore.collection("app_usage")
            .whereEqualTo("userId", userId)
            .whereEqualTo("packageName", packageName)
            .get().await()
        
        return snapshot.documents.firstOrNull()?.toObject(AppUsage::class.java)
    }
    
    suspend fun setDailyLimit(userId: String, packageName: String, appName: String, limitInMinutes: Int) {
        val appUsage = AppUsage(
            id = "${userId}_${packageName}",
            userId = userId,
            packageName = packageName,
            appName = appName,
            dailyLimit = limitInMinutes * 60 * 1000L // dakikayı milisaniyeye çevir
        )
        
        saveAppUsage(appUsage)
    }
    
    suspend fun updateUsedTime(userId: String, packageName: String, usedTime: Long) {
        val existingUsage = getAppUsageByPackage(userId, packageName)
        
        if (existingUsage != null) {
            // Mevcut kullanım süresine ekle
            val updatedUsage = existingUsage.copy(
                usedTime = existingUsage.usedTime + usedTime,
                lastUsed = System.currentTimeMillis(),
                isBlocked = (existingUsage.usedTime + usedTime) >= existingUsage.dailyLimit
            )
            updateAppUsage(updatedUsage)
        } else {
            // Yeni kullanım kaydı oluştur
            val packageManager = context.packageManager
            val appName = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0)
                ).toString()
            } catch (e: Exception) {
                packageName
            }
            
            val newUsage = AppUsage(
                id = "${userId}_${packageName}",
                userId = userId,
                packageName = packageName,
                appName = appName,
                usedTime = usedTime,
                dailyLimit = 0, // Henüz limit yok
                lastUsed = System.currentTimeMillis(),
                isBlocked = false
            )
            saveAppUsage(newUsage)
        }
    }
    
    suspend fun resetDailyUsage(userId: String) {
        val userApps = getAppUsageByUser(userId)
        
        userApps.forEach { appUsage ->
            val resetUsage = appUsage.copy(
                usedTime = 0,
                isBlocked = false
            )
            updateAppUsage(resetUsage)
        }
    }
}
