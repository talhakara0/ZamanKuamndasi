package com.example.zamankumandasi.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.zamankumandasi.R
import com.example.zamankumandasi.data.model.UserType
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel
import androidx.navigation.fragment.NavHostFragment
import com.example.zamankumandasi.ads.AdManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    private val adHandler = Handler(Looper.getMainLooper())
    private var inScreenAdRunnable: Runnable? = null
    private val UI_STAY_DELAY_MS = 45_000L // 45 saniye ekranda kalınca bir kez dene
    
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
                    AdManager.setPremium(false)
                }
                is AuthViewModel.AuthState.Success -> {
                    // Başarılı giriş sonrası flag'i resetle
                    isLoggedOut = false
                    // Premium durumunu reklam yöneticisine ilet
                    AdManager.setPremium(authState.user.isPremium)
                    // Premium durumundan hemen sonra reklam denemesi yapma (yarış koşulu riskini azalt)
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
                    // Premium durumunu reklam yöneticisine ilet
                    AdManager.setPremium(user.isPremium)
                    when (user.userType) {
                        UserType.PARENT -> {
                            navController.navigate(R.id.action_loginFragment_to_parentDashboardFragment)
                            if (!user.isPremium) AdManager.maybeShowInterstitial(this)
                        }
                        UserType.CHILD -> {
                            if (user.parentId == null) {
                                navController.navigate(R.id.action_loginFragment_to_pairingFragment)
                                if (!user.isPremium) AdManager.maybeShowInterstitial(this)
                            } else {
                                navController.navigate(R.id.action_loginFragment_to_childDashboardFragment)
                                if (!user.isPremium) AdManager.maybeShowInterstitial(this)
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
    
    override fun onResume() {
        super.onResume()
        scheduleInScreenAd()
    }

    override fun onPause() {
        super.onPause()
        cancelInScreenAd()
    }

    private fun scheduleInScreenAd() {
        cancelInScreenAd()
        // Premium ise hiçbir zaman planlama yapma
    if (AdManager.isPremiumEnabled()) return
        inScreenAdRunnable = Runnable {
            if (authViewModel.currentUser.value?.isPremium != true) {
                AdManager.maybeShowInterstitial(this)
            }
        }
        adHandler.postDelayed(inScreenAdRunnable!!, UI_STAY_DELAY_MS)
    }

    private fun cancelInScreenAd() {
        inScreenAdRunnable?.let { adHandler.removeCallbacks(it) }
        inScreenAdRunnable = null
    }
    
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
