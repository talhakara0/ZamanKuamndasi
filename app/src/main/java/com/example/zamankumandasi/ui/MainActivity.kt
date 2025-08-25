package com.example.zamankumandasi.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.zamankumandasi.R
import com.example.zamankumandasi.data.model.UserType
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // AuthState'i observe et - logout durumunu takip et
        var isLoggedOut = false
        authViewModel.authState.observe(this) { authState ->
            when (authState) {
                is AuthViewModel.AuthState.SignedOut -> {
                    android.util.Log.d("talha", "Kullanıcı çıkış yaptı - otomatik yönlendirme engellendi")
                    isLoggedOut = true
                }
                is AuthViewModel.AuthState.Success -> {
                    // Başarılı giriş sonrası flag'i resetle
                    isLoggedOut = false
                }
                else -> { /* Diğer durumlar */ }
            }
        }
        
        // Kullanıcı durumunu kontrol et ve uygun destination'a yönlendir
        authViewModel.currentUser.observe(this) { user ->
            // Eğer logout yapıldıysa otomatik yönlendirme yapma
            if (user != null && navController.currentDestination?.id == R.id.loginFragment && !isLoggedOut) {
                try {
                    android.util.Log.d("talha", "Otomatik yönlendirme yapılıyor: ${user.userType}")
                    when (user.userType) {
                        UserType.PARENT -> {
                            navController.navigate(R.id.action_loginFragment_to_parentDashboardFragment)
                        }
                        UserType.CHILD -> {
                            if (user.parentId == null) {
                                navController.navigate(R.id.action_loginFragment_to_pairingFragment)
                            } else {
                                navController.navigate(R.id.action_loginFragment_to_childDashboardFragment)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("talha", "Auto-navigation error: ${e.message}")
                }
            } else if (user == null) {
                android.util.Log.d("talha", "Kullanıcı null - login ekranında kalınıyor")
            } else if (isLoggedOut) {
                android.util.Log.d("talha", "Logout yapıldı - otomatik yönlendirme engellendi")
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
