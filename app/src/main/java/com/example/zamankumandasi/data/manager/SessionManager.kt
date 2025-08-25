package com.example.zamankumandasi.data.manager

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "zaman_kumandasi_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_LAST_LOGIN_TIME = "last_login_time"
        private const val KEY_PARENT_ID = "parent_id"
        private const val KEY_PAIRING_CODE = "pairing_code"
        private const val KEY_OFFLINE_MODE = "offline_mode"
    }
    
    fun setLoginSession(userId: String, email: String, userType: String, parentId: String? = null, pairingCode: String? = null) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_TYPE, userType)
            putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
            putString(KEY_PARENT_ID, parentId)
            putString(KEY_PAIRING_CODE, pairingCode)
            apply()
        }
    }
    
    fun clearSession() {
        prefs.edit().clear().apply()
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    fun getUserType(): String? {
        return prefs.getString(KEY_USER_TYPE, null)
    }
    
    fun getLastLoginTime(): Long {
        return prefs.getLong(KEY_LAST_LOGIN_TIME, 0)
    }
    
    fun getParentId(): String? {
        return prefs.getString(KEY_PARENT_ID, null)
    }
    
    fun getPairingCode(): String? {
        return prefs.getString(KEY_PAIRING_CODE, null)
    }
    
    fun setOfflineMode(isOffline: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_MODE, isOffline).apply()
    }
    
    fun isOfflineMode(): Boolean {
        return prefs.getBoolean(KEY_OFFLINE_MODE, false)
    }
    
    // Otomatik çıkış özelliği kaldırıldı - kullanıcı manuel çıkış yapana kadar session aktif kalır
    fun isSessionValid(): Boolean {
        return isLoggedIn() // Sadece login durumunu kontrol et, süre kontrolü yok
    }
}
