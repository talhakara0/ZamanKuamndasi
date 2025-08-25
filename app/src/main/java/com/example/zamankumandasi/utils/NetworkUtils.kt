package com.example.zamankumandasi.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class NetworkUtils(
    private val context: Context
) {
    
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                
                // Basit kontrol - sadece aktif bağlantı var mı?
                val hasConnection = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                
                android.util.Log.d("talha", "Network connection available: $hasConnection")
                return hasConnection
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                val isConnected = networkInfo?.isConnected == true
                android.util.Log.d("talha", "Legacy check - Connected: $isConnected, Type: ${networkInfo?.typeName}")
                return isConnected
            }
        } catch (e: Exception) {
            android.util.Log.e("talha", "Error checking network: ${e.message}")
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

    // En basit ve güvenilir internet kontrolü
    fun hasNetworkConnection(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                if (network == null) {
                    android.util.Log.d("talha", "Aktif network yok")
                    return false
                }
                
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities == null) {
                    android.util.Log.d("talha", "Network capabilities yok")
                    return false
                }
                
                // Sadece bağlantı türü kontrolü - EN BASİT KONTROL
                val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                val hasEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                
                val hasAnyConnection = hasWifi || hasCellular || hasEthernet
                
                android.util.Log.d("talha", "WiFi: $hasWifi, Cellular: $hasCellular, Ethernet: $hasEthernet, Final: $hasAnyConnection")
                return hasAnyConnection
                
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                val isConnected = networkInfo?.isConnected == true
                android.util.Log.d("talha", "Legacy - Connected: $isConnected")
                return isConnected
            }
        } catch (e: Exception) {
            android.util.Log.e("talha", "Network kontrol hatası: ${e.message}")
            // Hata durumunda true döndür - kullanıcıyı rahatsız etme
            return true
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
                } else {
                    builder.append("No active network\n")
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                builder.append("Legacy Network Info: ${networkInfo?.toString()}\n")
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
