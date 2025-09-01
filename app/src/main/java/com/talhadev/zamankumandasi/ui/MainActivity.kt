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
                                
                                // Çocuk hesabı için tüm izinleri kontrol et
                                checkAllRequiredPermissions()
                                
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
        
        // Overlay izni kontrolü - uygulama geri geldiğinde
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                android.util.Log.d("MainActivity", "onResume: Overlay izni mevcut!")
                // İzinler tamam mı kontrol et
                checkAllRequiredPermissions()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cancelInScreenAd()
    }

    private fun checkAllRequiredPermissions() {
        val permissionStatus = PermissionHelper.checkAllRequiredPermissions(this)
        
        if (!permissionStatus.allGranted) {
            android.util.Log.d("MainActivity", "İzinler eksik - kontrol ediliyor")
            
            // Overlay izni özel kontrolü
            if (!permissionStatus.overlay) {
                requestOverlayPermission()
            } else {
                // Diğer izinler için dialog göster
                PermissionCheckDialog.newInstance {
                    android.util.Log.d("MainActivity", "Tüm izinler verildi!")
                }.show(supportFragmentManager, "PermissionCheckDialog")
            }
        } else {
            android.util.Log.d("MainActivity", "Tüm izinler zaten verilmiş!")
        }
    }

    private fun requestOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    android.util.Log.d("MainActivity", "Overlay izni verildi!")
                    
                    // Kısa bir bekleme sonrası yeniden kontrol et
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        android.util.Log.d("MainActivity", "Overlay izni yeniden kontrol ediliyor...")
                        checkAllRequiredPermissions()
                    }, 1000) // 1 saniye bekle
                    
                } else {
                    android.util.Log.d("MainActivity", "Overlay izni reddedildi!")
                    // Kullanıcıya uyarı göster
                    android.widget.Toast.makeText(
                        this,
                        "Overlay izni gerekli! Lütfen izin verin ve uygulamayı yeniden başlatın.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
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

    private fun showAccessibilityServiceDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Erişilebilirlik Servisi Gerekli")
            .setMessage("Çocuk hesaplarında limit dolmuş uygulamaları engellemek için Erişilebilirlik servisini etkinleştirmeniz gerekiyor.")
            .setPositiveButton("Ayarları Aç") { _, _ ->
                AccessibilityHelper.openAccessibilitySettings(this)
            }
            .setNegativeButton("Daha Sonra") { _, _ -> }
            .setCancelable(false)
            .show()
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
