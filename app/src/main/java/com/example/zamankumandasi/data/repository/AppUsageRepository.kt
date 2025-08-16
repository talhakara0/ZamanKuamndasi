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
import android.util.Log

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
        Log.d("talha", "saveAppUsage çağrıldı: $appUsage")
        try {
            database.reference.child("app_usage").child(appUsage.id).setValue(appUsage).await()
            Log.d("talha", "saveAppUsage başarılı: ${appUsage.id}")
        } catch (e: Exception) {
            Log.e("talha", "saveAppUsage hata: ${e.message}")
            throw e
        }
    }
    
    suspend fun getAppUsageByUser(userId: String): List<AppUsage> {
        Log.d("talha", "getAppUsageByUser çağrıldı: userId=$userId")
        try {
            val snapshot = database.reference.child("app_usage").orderByChild("userId").equalTo(userId).get().await()
            val result = snapshot.children.mapNotNull { it.getValue(AppUsage::class.java) }
            Log.d("talha", "getAppUsageByUser başarılı: ${result.size} kayıt bulundu")
            return result
        } catch (e: Exception) {
            Log.e("talha", "getAppUsageByUser hata: ${e.message}")
            return emptyList()
        }
    }
    
    suspend fun updateAppUsage(appUsage: AppUsage) {
        Log.d("talha", "updateAppUsage çağrıldı: $appUsage")
        try {
            database.reference.child("app_usage").child(appUsage.id).setValue(appUsage).await()
            Log.d("talha", "updateAppUsage başarılı: ${appUsage.id}")
        } catch (e: Exception) {
            Log.e("talha", "updateAppUsage hata: ${e.message}")
        }
    }
    
    suspend fun getAppUsageByPackage(userId: String, packageName: String): AppUsage? {
        Log.d("talha", "getAppUsageByPackage çağrıldı: userId=$userId, packageName=$packageName")
        try {
            val snapshot = database.reference.child("app_usage").orderByChild("userId").equalTo(userId).get().await()
            val result = snapshot.children.mapNotNull { it.getValue(AppUsage::class.java) }.firstOrNull { it.packageName == packageName }
            Log.d("talha", "getAppUsageByPackage başarılı: $result")
            return result
        } catch (e: Exception) {
            Log.e("talha", "getAppUsageByPackage hata: ${e.message}")
            return null
        }
    }
    
    suspend fun setDailyLimit(userId: String, packageName: String, appName: String, limitInMinutes: Int) {
        Log.d("talha", "setDailyLimit çağrıldı: userId=$userId, packageName=$packageName, appName=$appName, limitInMinutes=$limitInMinutes")
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
        Log.d("talha", "updateDailyLimit çağrıldı: userId=$userId, packageName=$packageName, dailyLimit=$dailyLimit")
        return try {
            val existingUsage = getAppUsageByPackage(userId, packageName)
            if (existingUsage != null) {
                val updatedUsage = existingUsage.copy(
                    dailyLimit = dailyLimit,
                    isBlocked = existingUsage.usedTime >= dailyLimit && dailyLimit > 0
                )
                database.reference.child("app_usage").child(existingUsage.id).setValue(updatedUsage).await()
                Log.d("talha", "updateDailyLimit başarılı: ${existingUsage.id}")
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
                Log.d("talha", "updateDailyLimit yeni kayıt başarılı: ${newUsage.id}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("talha", "updateDailyLimit hata: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateUsedTime(userId: String, packageName: String, usedTime: Long) {
        Log.d("talha", "updateUsedTime çağrıldı: userId=$userId, packageName=$packageName, usedTime=$usedTime")
        try {
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
            Log.d("talha", "updateUsedTime başarılı")
        } catch (e: Exception) {
            Log.e("talha", "updateUsedTime hata: ${e.message}")
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
