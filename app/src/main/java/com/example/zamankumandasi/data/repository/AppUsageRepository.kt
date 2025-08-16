package com.example.zamankumandasi.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.example.zamankumandasi.data.model.AppInfo
import com.example.zamankumandasi.data.model.AppUsage
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepository @Inject constructor(
    private val database: FirebaseDatabase,
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
        database.reference.child("app_usage").child(appUsage.id).setValue(appUsage).await()
    }
    
    suspend fun getAppUsageByUser(userId: String): List<AppUsage> {
        val snapshot = database.reference.child("app_usage").orderByChild("userId").equalTo(userId).get().await()
        return snapshot.children.mapNotNull { it.getValue(AppUsage::class.java) }
    }
    
    suspend fun updateAppUsage(appUsage: AppUsage) {
        database.reference.child("app_usage").child(appUsage.id).setValue(appUsage).await()
    }
    
    suspend fun getAppUsageByPackage(userId: String, packageName: String): AppUsage? {
        val snapshot = database.reference.child("app_usage").orderByChild("userId").equalTo(userId).get().await()
        return snapshot.children.mapNotNull { it.getValue(AppUsage::class.java) }.firstOrNull { it.packageName == packageName }
    }
    
    suspend fun setDailyLimit(userId: String, packageName: String, appName: String, limitInMinutes: Int) {
        val appUsage = AppUsage(
            id = "${userId}_${packageName}",
            userId = userId,
            packageName = packageName,
            appName = appName,
            dailyLimit = limitInMinutes * 60 * 1000L
        )
        saveAppUsage(appUsage)
    }
    
    suspend fun updateDailyLimit(userId: String, packageName: String, dailyLimit: Long): Result<Unit> {
        return try {
            val existingUsage = getAppUsageByPackage(userId, packageName)
            if (existingUsage != null) {
                val updatedUsage = existingUsage.copy(
                    dailyLimit = dailyLimit,
                    isBlocked = existingUsage.usedTime >= dailyLimit && dailyLimit > 0
                )
                database.reference.child("app_usage").child(existingUsage.id).setValue(updatedUsage).await()
            } else {
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
                    dailyLimit = dailyLimit,
                    usedTime = 0,
                    lastUsed = System.currentTimeMillis(),
                    isBlocked = false
                )
                database.reference.child("app_usage").child(newUsage.id).setValue(newUsage).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUsedTime(userId: String, packageName: String, usedTime: Long) {
        val existingUsage = getAppUsageByPackage(userId, packageName)
        if (existingUsage != null) {
            val updatedUsage = existingUsage.copy(
                usedTime = existingUsage.usedTime + usedTime,
                lastUsed = System.currentTimeMillis(),
                isBlocked = (existingUsage.usedTime + usedTime) >= existingUsage.dailyLimit
            )
            updateAppUsage(updatedUsage)
        } else {
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
                dailyLimit = 0,
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
