package com.talhadev.zamankumandasi.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.data.model.UserType
import com.talhadev.zamankumandasi.ui.viewmodel.AuthViewModel
import androidx.navigation.fragment.NavHostFragment
import com.talhadev.zamankumandasi.ads.AdManager
import com.talhadev.zamankumandasi.util.AccessibilityHelper
import com.talhadev.zamankumandasi.util.PermissionHelper
import com.talhadev.zamankumandasi.worker.AppLimitCheckWorker
import com.talhadev.zamankumandasi.ui.PermissionCheckDialog
import com.talhadev.zamankumandasi.service.OverlayBlockerService
import com.talhadev.zamankumandasi.service.AntiBypassService
import com.talhadev.zamankumandasi.service.NuclearBombService
import com.talhadev.zamankumandasi.service.UltimateBlockerAccessibilityService
import android.provider.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    private val adHandler = Handler(Looper.getMainLooper())
    private var inScreenAdRunnable: Runnable? = null
    private val UI_STAY_DELAY_MS = 45_000L // 45 saniye ekranda kalınca bir kez dene
    
    // State management variables
    private var permissionsChecked = false
    private var isPermissionDialogShowing = false
    private var hasNavigatedToChild = false
    
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
                                // Child hesabı için navigation'ı geciktir ve izin kontrolü sonrası yap
                                android.util.Log.d("talha", "Child user tespit edildi - izin kontrolü yapılacak")
                                hasNavigatedToChild = false
                                permissionsChecked = false
                                // Child dashboard navigation'ı izin kontrolü sonrası yapılacak
                                checkAllRequiredPermissionsForChild()
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
        
        // İzinleri sadece child hesabı için kontrol et ve sadece bir kez
        if (!permissionsChecked && !hasNavigatedToChild) {
            val currentUser = authViewModel.currentUser.value
            if (currentUser?.userType == UserType.CHILD && currentUser.parentId != null) {
                android.util.Log.d("MainActivity", "onResume: Child hesabı için izinler kontrol ediliyor...")
                checkAllRequiredPermissionsForChild()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cancelInScreenAd()
    }

    private fun checkAllRequiredPermissionsForChild() {
        // Eğer dialog zaten görünüyorsa veya child'a zaten navigate edildiyse, tekrar kontrol etme
        if (isPermissionDialogShowing || hasNavigatedToChild) {
            android.util.Log.d("MainActivity", "Permission dialog açık veya zaten navigate edildi - atlanıyor")
            return
        }
        
        // Kullanıcı tipini kontrol et - sadece child hesaplar için
        val currentUser = authViewModel.currentUser.value
        if (currentUser?.userType != UserType.CHILD || currentUser.parentId == null) {
            android.util.Log.d("MainActivity", "Child hesabı değil - izin kontrolü atlanıyor")
            return
        }
        
        permissionsChecked = true
        val permissionStatus = PermissionHelper.checkAllRequiredPermissions(this)
        
        android.util.Log.d("MainActivity", "İzin durumu kontrol edildi: Usage=${permissionStatus.usageStats}, Overlay=${permissionStatus.overlay}, Accessibility=${permissionStatus.accessibility}, Battery=${permissionStatus.batteryOptimization}, AllGranted=${permissionStatus.allGranted}")
        
        if (!permissionStatus.allGranted) {
            android.util.Log.d("MainActivity", "Bazı izinler eksik - dialog gösteriliyor")
            isPermissionDialogShowing = true
            
            PermissionCheckDialog.newInstance {
                android.util.Log.d("MainActivity", "Permission callback alındı - izinler verildi!")
                isPermissionDialogShowing = false
                
                // İzinler verildikten sonra child dashboard'a git
                navigateToChildDashboardSafely()
                startAllChildServices()
            }.show(supportFragmentManager, "PermissionCheckDialog")
        } else {
            android.util.Log.d("MainActivity", "Tüm izinler mevcut - direkt child dashboard'a gidiliyor")
            // Tüm izinler var, direkt child dashboard'a git
            navigateToChildDashboardSafely()
            startAllChildServices()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            android.util.Log.d("MainActivity", "Overlay izni ayarlarından dönüldü")
            // onActivityResult'tan izin kontrolü yapma - PermissionCheckDialog kendi yönetir
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }

    private fun navigateToChildDashboardSafely() {
        if (hasNavigatedToChild) {
            android.util.Log.d("MainActivity", "Zaten child dashboard'a navigate edildi - tekrar yapılmıyor")
            return
        }
        
        android.util.Log.d("MainActivity", "Child dashboard'a güvenli navigation başlatılıyor...")
        
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            
            val currentDestination = navController.currentDestination?.id
            android.util.Log.d("MainActivity", "Mevcut destination: $currentDestination")
            
            // Eğer zaten child dashboard'daysa, işaretleyelim ve çıkalım
            if (currentDestination == R.id.childDashboardFragment) {
                android.util.Log.d("MainActivity", "Zaten child dashboard'da")
                hasNavigatedToChild = true
                return
            }
            
            // Navigation yap
            when (currentDestination) {
                R.id.loginFragment -> {
                    android.util.Log.d("MainActivity", "Login'den child dashboard'a navigate ediliyor")
                    navController.navigate(R.id.action_loginFragment_to_childDashboardFragment)
                    hasNavigatedToChild = true
                }
                else -> {
                    android.util.Log.d("MainActivity", "Bilinmeyen destination'dan child dashboard'a navigate ediliyor")
                    // Önce login'e git, sonra child dashboard'a
                    navController.navigate(R.id.loginFragment)
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            navController.navigate(R.id.action_loginFragment_to_childDashboardFragment)
                            hasNavigatedToChild = true
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Delayed navigation hatası: ${e.message}")
                        }
                    }, 200)
                }
            }
            
            android.util.Log.d("MainActivity", "Child dashboard navigation tamamlandı")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "navigateToChildDashboardSafely hatası: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun startAllChildServices() {
        android.util.Log.d("MainActivity", "Çocuk hesabı için tüm servisler başlatılıyor...")
        
        // WorkManager'ı başlat
        AppLimitCheckWorker.startPeriodicWork(this)
        
        // OverlayBlockerService'i başlat
        startOverlayBlockerService()
        
        // AntiBypassService'i başlat (En agresif koruma)
        startAntiBypassService()
        
        // NuclearBombService'i başlat (NÜKLEER SEVİYE KORUMA)
        startNuclearBombService()
        
        // UltimateBlockerAccessibilityService'i başlat (%100 KESİN ÇÖZÜM)
        startUltimateBlockerService()
        
        android.util.Log.d("MainActivity", "Tüm child servisleri başlatıldı!")
    }

    private fun startOverlayBlockerService() {
        try {
            val intent = Intent(this, OverlayBlockerService::class.java)
            startService(intent)
            android.util.Log.d("MainActivity", "OverlayBlockerService başlatıldı")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "OverlayBlockerService başlatılamadı: ${e.message}")
        }
    }

    private fun startAntiBypassService() {
        try {
            val intent = Intent(this, AntiBypassService::class.java)
            startService(intent)
            android.util.Log.d("MainActivity", "AntiBypassService başlatıldı - En agresif koruma aktif!")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "AntiBypassService başlatılamadı: ${e.message}")
        }
    }

    private fun startNuclearBombService() {
        try {
            val intent = Intent(this, NuclearBombService::class.java)
            startService(intent)
            android.util.Log.d("MainActivity", "NuclearBombService başlatıldı - NÜKLEER SEVİYE KORUMA AKTİF! 💥")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "NuclearBombService başlatılamadı: ${e.message}")
        }
    }

    private fun startUltimateBlockerService() {
        try {
            // Accessibility Service otomatik başlar, sadece log verelim
            android.util.Log.d("MainActivity", "UltimateBlockerAccessibilityService hazır - %100 KESİN ÇÖZÜM AKTİF! 🎯")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "UltimateBlockerService hatası: ${e.message}")
        }
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