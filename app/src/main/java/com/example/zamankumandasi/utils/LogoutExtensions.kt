package com.example.zamankumandasi.utils

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.zamankumandasi.data.manager.LogoutManager
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel

/**
 * Fragment için logout işlemini kolaylaştıran extension fonksiyonları
 */

fun Fragment.performLogoutWithManager(
    authViewModel: AuthViewModel,
    logoutManager: LogoutManager,
    showConfirmation: Boolean = true,
    customMessage: String? = null
) {
    logoutManager.performLogout(
        authViewModel = authViewModel,
        navController = findNavController(),
        showConfirmation = showConfirmation,
        onLogoutStarted = {
            // Loading gösterilebilir
        },
        onLogoutCompleted = {
            val message = customMessage ?: "Başarıyla çıkış yapıldı"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
        onLogoutError = { error ->
            Toast.makeText(context, "Çıkış hatası: $error", Toast.LENGTH_LONG).show()
        }
    )
}

fun Fragment.performQuickLogout(
    authViewModel: AuthViewModel,
    logoutManager: LogoutManager
) {
    performLogoutWithManager(
        authViewModel = authViewModel,
        logoutManager = logoutManager,
        showConfirmation = false,
        customMessage = "Oturum sonlandırıldı"
    )
}
