package com.example.zamankumandasi.data.manager

import android.content.Context
import android.util.Log
import androidx.navigation.NavController
import com.example.zamankumandasi.R
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogoutManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun performLogout(
        authViewModel: AuthViewModel,
        navController: NavController,
        showConfirmation: Boolean = true,
        onLogoutStarted: (() -> Unit)? = null,
        onLogoutCompleted: (() -> Unit)? = null,
        onLogoutError: ((String) -> Unit)? = null
    ) {
        Log.d("talha", "LogoutManager: performLogout çağrıldı, showConfirmation=$showConfirmation")
        
        if (showConfirmation) {
            Log.d("talha", "LogoutManager: Onay dialogu gösteriliyor")
            showLogoutConfirmationDialog(
                context = context,
                onConfirmed = {
                    Log.d("talha", "LogoutManager: Dialog onaylandı, logout işlemi başlatılıyor")
                    executeLogout(authViewModel, navController, onLogoutStarted, onLogoutCompleted, onLogoutError)
                }
            )
        } else {
            executeLogout(authViewModel, navController, onLogoutStarted, onLogoutCompleted, onLogoutError)
        }
    }
    
    private fun executeLogout(
        authViewModel: AuthViewModel,
        navController: NavController,
        onLogoutStarted: (() -> Unit)?,
        onLogoutCompleted: (() -> Unit)?,
        onLogoutError: ((String) -> Unit)?
    ) {
        onLogoutStarted?.invoke()
        
        try {
            // AuthViewModel'den logout işlemini başlat
            authViewModel.signOut()
            
            // Biraz bekle ki logout işlemi tamamlansın
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    navigateToLogin(navController)
                    onLogoutCompleted?.invoke()
                } catch (e: Exception) {
                    onLogoutError?.invoke("Navigation hatası: ${e.message}")
                }
            }, 500)
            
        } catch (e: Exception) {
            onLogoutError?.invoke("Logout hatası: ${e.message}")
        }
    }
    
    private fun navigateToLogin(navController: NavController) {
        try {
            val currentDestination = navController.currentDestination?.id
            
            when (currentDestination) {
                R.id.childDashboardFragment -> {
                    navController.navigate(R.id.action_childDashboardFragment_to_loginFragment)
                }
                R.id.parentDashboardFragment -> {
                    navController.navigate(R.id.action_parentDashboardFragment_to_loginFragment)
                }
                else -> {
                    // Diğer durumlar için genel navigation
                    navController.navigate(
                        R.id.loginFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                }
            }
        } catch (e: Exception) {
            // Navigation hatası durumunda alternative yol dene
            try {
                navController.popBackStack(R.id.loginFragment, false)
            } catch (e2: Exception) {
                // Son çare olarak direkt login fragment'a git
                navController.navigate(R.id.loginFragment)
            }
        }
    }
    
    private fun showLogoutConfirmationDialog(
        context: Context,
        onConfirmed: () -> Unit
    ) {
        try {
            Log.d("talha", "LogoutManager: AlertDialog oluşturuluyor")
            // Basit Android AlertDialog kullan - tema sorunu olmaz
            android.app.AlertDialog.Builder(context)
                .setTitle("Çıkış Yap")
                .setMessage("Çıkış yapmak istediğinize emin misiniz?")
                .setPositiveButton("Evet") { dialog, _ ->
                    Log.d("talha", "LogoutManager: Evet butonuna tıklandı")
                    dialog.dismiss()
                    onConfirmed.invoke()
                }
                .setNegativeButton("Hayır") { dialog, _ ->
                    Log.d("talha", "LogoutManager: Hayır butonuna tıklandı")
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e("talha", "LogoutManager: AlertDialog hatası: ${e.message}")
            // Hata durumunda direkt çıkış yap
            onConfirmed.invoke()
        }
    }
}
