package com.example.zamankumandasi.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                if (network == null) {
                    android.util.Log.d("NetworkUtils", "No active network")
                    return false
                }
                
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities == null) {
                    android.util.Log.d("NetworkUtils", "No network capabilities")
                    return false
                }
                
                val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                val hasEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                android.util.Log.d("NetworkUtils", "WiFi: $hasWifi, Cellular: $hasCellular, Ethernet: $hasEthernet, Internet: $hasInternet, Validated: $isValidated")
                
                return (hasWifi || hasCellular || hasEthernet) && hasInternet && isValidated
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                val isConnected = networkInfo?.isConnected == true
                android.util.Log.d("NetworkUtils", "Legacy check - Connected: $isConnected, Type: ${networkInfo?.typeName}")
                return isConnected
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkUtils", "Error checking network: ${e.message}")
            return false
        }
    }
    
    fun getNetworkStatus(): NetworkStatus {
        return if (isNetworkAvailable()) {
            NetworkStatus.ONLINE
        } else {
            NetworkStatus.OFFLINE
        }
    }
    
    // Daha basit internet kontrolü (sadece bağlantı var mı yok mu)
    fun hasNetworkConnection(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkUtils", "Error checking simple network: ${e.message}")
            false
        }
    }
    
    // Test için - gerçek internet bağlantısını ping ile kontrol et
    fun canReachInternet(): Boolean {
        return try {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            android.util.Log.e("NetworkUtils", "Ping test failed: ${e.message}")
            false
        }
    }
    // Debug için network bilgilerini detaylı yazdıran metod
    fun getDetailedNetworkInfo(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val builder = StringBuilder()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                builder.append("Active Network: ${network != null}\n")
                
                if (network != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    if (capabilities != null) {
                        builder.append("WiFi: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}\n")
                        builder.append("Cellular: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)}\n")
                        builder.append("Ethernet: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)}\n")
                        builder.append("Internet: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}\n")
                        builder.append("Validated: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}\n")
                    } else {
                        builder.append("No capabilities\n")
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                builder.append("Legacy - Connected: ${networkInfo?.isConnected}\n")
                builder.append("Legacy - Type: ${networkInfo?.typeName}\n")
            }
            
            builder.toString()
        } catch (e: Exception) {
            "Error getting network info: ${e.message}"
        }
    }
}

enum class NetworkStatus {
    ONLINE,
    OFFLINE
}
