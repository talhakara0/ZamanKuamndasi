package com.talhadev.zamankumandasi.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.util.PermissionHelper

class PermissionCheckDialog : DialogFragment() {
    
    private var permissionStatus: PermissionHelper.PermissionStatus? = null
    private var onPermissionsGranted: (() -> Unit)? = null
    private var hasCheckedOnce = false // Otomatik kontrol bayrağı
    private var hasTriggeredCallback = false // Callback tetiklendi mi?
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialog)
        // Dialog'u kapatmayı engelle - sadece izinler verildikten sonra kapanabilir
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_permission_check, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.i("PermissionCheckDialog", "🔍 DIALOG AÇILDI - İZİNLER KONTROL EDİLİYOR...")
        permissionStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
        
        android.util.Log.i("PermissionCheckDialog", "📊 Dialog'da izin durumu:")
        android.util.Log.i("PermissionCheckDialog", "📊 Usage Stats: ${permissionStatus?.usageStats}")
        android.util.Log.i("PermissionCheckDialog", "🖼️ Overlay: ${permissionStatus?.overlay}")
        android.util.Log.i("PermissionCheckDialog", "♿ Accessibility: ${permissionStatus?.accessibility}")
        android.util.Log.i("PermissionCheckDialog", "🔋 Battery: ${permissionStatus?.batteryOptimization}")
        android.util.Log.i("PermissionCheckDialog", "🎯 TÜM İZİNLER VERİLDİ Mİ? ${permissionStatus?.allGranted}")
        
        setupUI()
    }
    
    private fun setupUI() {
        val status = permissionStatus ?: return
        
        android.util.Log.d("PermissionCheckDialog", "UI kuruluyor - İzin durumu: $status")
        
        // Kullanım istatistikleri
        val tvUsageStats = view?.findViewById<TextView>(R.id.tvUsageStats)
        val ivUsageStats = view?.findViewById<ImageView>(R.id.ivUsageStats)
        val btnUsageStats = view?.findViewById<Button>(R.id.btnUsageStats)
        
        tvUsageStats?.text = if (status.usageStats) {
            "✅ Kullanım istatistikleri izni verildi"
        } else {
            "❌ Kullanım istatistikleri izni gerekli"
        }
        
        ivUsageStats?.setImageResource(
            if (status.usageStats) android.R.drawable.ic_menu_send else android.R.drawable.ic_menu_close_clear_cancel
        )
        
        btnUsageStats?.setOnClickListener {
            PermissionHelper.openUsageStatsSettings(requireContext())
        }
        
        // Overlay izni
        val tvOverlay = view?.findViewById<TextView>(R.id.tvOverlay)
        val ivOverlay = view?.findViewById<ImageView>(R.id.ivOverlay)
        val btnOverlay = view?.findViewById<Button>(R.id.btnOverlay)
        
        tvOverlay?.text = if (status.overlay) {
            "✅ Overlay izni verildi"
        } else {
            "❌ Diğer uygulamalar üstünde gösterme izni gerekli"
        }
        
        ivOverlay?.setImageResource(
            if (status.overlay) android.R.drawable.ic_menu_send else android.R.drawable.ic_menu_close_clear_cancel
        )
        
        btnOverlay?.setOnClickListener {
            PermissionHelper.openOverlaySettings(requireContext())
        }
        
        // Accessibility Service izni
        val tvAccessibility = view?.findViewById<TextView>(R.id.tvAccessibility)
        val ivAccessibility = view?.findViewById<ImageView>(R.id.ivAccessibility)
        val btnAccessibility = view?.findViewById<Button>(R.id.btnAccessibility)
        
        tvAccessibility?.text = if (status.accessibility) {
            "✅ Accessibility Service izni verildi"
        } else {
            "❌ Accessibility Service izni gerekli"
        }
        
        ivAccessibility?.setImageResource(
            if (status.accessibility) android.R.drawable.ic_menu_send else android.R.drawable.ic_menu_close_clear_cancel
        )
        
        btnAccessibility?.setOnClickListener {
            PermissionHelper.openAccessibilitySettings(requireContext())
        }
        
        // Battery optimizasyonu
        val tvBattery = view?.findViewById<TextView>(R.id.tvBattery)
        val ivBattery = view?.findViewById<ImageView>(R.id.ivBattery)
        val btnBattery = view?.findViewById<Button>(R.id.btnBattery)
        
        tvBattery?.text = if (status.batteryOptimization) {
            "✅ Battery optimizasyonu kapatıldı"
        } else {
            "❌ Battery optimizasyonu kapatılmalı"
        }
        
        ivBattery?.setImageResource(
            if (status.batteryOptimization) android.R.drawable.ic_menu_send else android.R.drawable.ic_menu_close_clear_cancel
        )
        
        btnBattery?.setOnClickListener {
            PermissionHelper.openBatteryOptimizationSettings(requireContext())
        }
        

        
        // Yenile butonu
        val btnRefresh = view?.findViewById<Button>(R.id.btnRefresh)
        btnRefresh?.setOnClickListener {
            refreshPermissions()
        }
        
        // Devam et butonu (sadece tüm izinler verildiğinde görünür)
        val btnContinue = view?.findViewById<Button>(R.id.btnContinue)
        if (status.allGranted) {
            btnContinue?.visibility = View.VISIBLE
            btnContinue?.text = "🎉 Devam Et - Tüm İzinler Verildi!"
            btnContinue?.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Yeşil
            btnContinue?.setTextColor(android.graphics.Color.WHITE)
        } else {
            btnContinue?.visibility = View.GONE
        }
        btnContinue?.setOnClickListener {
            android.util.Log.i("PermissionCheckDialog", "🔘 Devam et butonuna tıklandı")
            
            // Butona tıklandığında da izin kontrolü yap
            val currentStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
            android.util.Log.i("PermissionCheckDialog", "🔍 BUTON TIKLAMA İZİN KONTROLÜ:")
            android.util.Log.i("PermissionCheckDialog", "📊 Usage: ${currentStatus.usageStats}, Overlay: ${currentStatus.overlay}, Accessibility: ${currentStatus.accessibility}, Battery: ${currentStatus.batteryOptimization}")
            android.util.Log.i("PermissionCheckDialog", "🎯 TÜM İZİNLER VERİLDİ Mİ? ${currentStatus.allGranted}")
            
            if (!currentStatus.allGranted) {
                android.util.Log.e("PermissionCheckDialog", "❌ HATA: TÜM İZİNLER VERİLMEDİ AMA DEVAM ET BUTONU GÖRÜNÜYOR!")
                android.util.Log.e("PermissionCheckDialog", "❌ Bu durumda buton görünmemeli!")
                return@setOnClickListener
            }
            
            triggerCallbackAndDismiss()
        }
        
        // Açıklama metni
        val tvDescription = view?.findViewById<TextView>(R.id.tvDescription)
        tvDescription?.text = if (status.allGranted) {
            "🎉 Tüm temel izinler verildi! Uygulama engelleme sistemi çalışmaya hazır."
        } else {
            "⚠️ UYARI: Uygulama engelleme sisteminin çalışması için aşağıdaki temel izinlerin VERİLMESİ ZORUNLUDUR!\n\n" +
            "Bu izinler verilmeden uygulamaya erişim sağlanamaz. Lütfen tüm izinleri verin."
        }
        
        // Yardım metinleri
        val tvAccessibilityHelp = view?.findViewById<TextView>(R.id.tvAccessibilityHelp)
        if (!status.overlay || !status.accessibility) {
            tvAccessibilityHelp?.visibility = View.VISIBLE
            var helpText = "💡 Yardım:\n\n"
            
            if (!status.overlay) {
                helpText += "🔹 Overlay izni için:\n" +
                    "1. Ayarlar > Uygulamalar > ZamanKumandasi'ne gidin\n" +
                    "2. 'Diğer uygulamalar üzerinde görüntüle' seçeneğini bulun\n" +
                    "3. Switch'i açın\n\n"
            }
            
            if (!status.accessibility) {
                helpText += "🔹 Accessibility Service için:\n" +
                    "1. Ayarlar > Erişilebilirlik'e gidin\n" +
                    "2. 'ZamanKumandasi Uygulama Engelleme' servisini bulun\n" +
                    "3. Switch'i açın ve 'Tamam' deyin\n" +
                    "4. Bu izin sayesinde uygulamaları engelleyebiliriz\n\n"
            }
            
            helpText += "⚠️ TÜM İZİNLER ZORUNLUDUR! Uygulama engelleme sistemi çalışması için hepsi gerekli."
            
            tvAccessibilityHelp?.text = helpText
        } else {
            tvAccessibilityHelp?.visibility = View.GONE
        }
        
        // UI kurulduktan sonra otomatik kontrol - sadece bir kez
        if (!hasCheckedOnce && status.allGranted) {
            hasCheckedOnce = true
            android.util.Log.d("PermissionCheckDialog", "UI kuruldu ve tüm izinler var - otomatik devam")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                triggerCallbackAndDismiss()
            }, 800) // 800ms bekle - UI'ın tamamen kurulması için
        } else if (!hasCheckedOnce) {
            hasCheckedOnce = true
            android.util.Log.d("PermissionCheckDialog", "UI kuruldu - eksik izinler var")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Ayarlardan dönüldüğünde izinleri yenile
        android.util.Log.d("PermissionCheckDialog", "onResume - İzinler kontrol ediliyor...")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isAdded && !isRemoving) {
                refreshPermissions()
            }
        }, 600) // 600ms bekle - ayarlar ekranından dönme zamanı
    }
    
    private fun refreshPermissions() {
        if (hasTriggeredCallback) {
            android.util.Log.w("PermissionCheckDialog", "⚠️ Callback zaten tetiklendi - refresh atlanıyor")
            return
        }
        
        android.util.Log.i("PermissionCheckDialog", "🔄 İZİNLER YENİLENİYOR...")
        permissionStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
        
        // Yeni permission status'u kontrol et
        val newStatus = permissionStatus
        if (newStatus != null) {
            android.util.Log.i("PermissionCheckDialog", "📊 YENİ İZİN DURUMU:")
            android.util.Log.i("PermissionCheckDialog", "📊 Usage Stats: ${newStatus.usageStats}")
            android.util.Log.i("PermissionCheckDialog", "🖼️ Overlay: ${newStatus.overlay}")
            android.util.Log.i("PermissionCheckDialog", "♿ Accessibility: ${newStatus.accessibility}")
            android.util.Log.i("PermissionCheckDialog", "🔋 Battery: ${newStatus.batteryOptimization}")
            android.util.Log.i("PermissionCheckDialog", "🎯 TÜM İZİNLER VERİLDİ Mİ? ${newStatus.allGranted}")
        }
        
        setupUI()
        
        // SADECE TÜM İZİNLER VERİLDİYSE OTOMATİK OLARAK DEVAM ET
        if (permissionStatus?.allGranted == true) {
            android.util.Log.i("PermissionCheckDialog", "✅ REFRESH SONRASI TÜM İZİNLER MEVCUT - DEVAM EDİLİYOR")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                triggerCallbackAndDismiss()
            }, 1000) // 1 saniye bekle - kullanıcının görmesi için
        } else {
            android.util.Log.w("PermissionCheckDialog", "⚠️ REFRESH SONRASI HALA EKSİK İZİNLER VAR!")
            android.util.Log.w("PermissionCheckDialog", "⚠️ Usage: ${permissionStatus?.usageStats}, Overlay: ${permissionStatus?.overlay}, Accessibility: ${permissionStatus?.accessibility}, Battery: ${permissionStatus?.batteryOptimization}")
        }
    }
    
    private fun triggerCallbackAndDismiss() {
        if (hasTriggeredCallback) {
            android.util.Log.w("PermissionCheckDialog", "⚠️ Callback zaten tetiklendi - tekrar tetiklenmiyor")
            return
        }
        
        if (!isAdded || isRemoving) {
            android.util.Log.w("PermissionCheckDialog", "⚠️ Dialog durumu uygun değil - callback atlanıyor")
            return
        }
        
        // Son bir kez izin kontrolü yap
        val finalStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
        android.util.Log.i("PermissionCheckDialog", "🔍 SON İZİN KONTROLÜ:")
        android.util.Log.i("PermissionCheckDialog", "📊 Usage: ${finalStatus.usageStats}, Overlay: ${finalStatus.overlay}, Accessibility: ${finalStatus.accessibility}, Battery: ${finalStatus.batteryOptimization}")
        android.util.Log.i("PermissionCheckDialog", "🎯 TÜM İZİNLER VERİLDİ Mİ? ${finalStatus.allGranted}")
        
        if (!finalStatus.allGranted) {
            android.util.Log.e("PermissionCheckDialog", "❌ HATA: TÜM İZİNLER VERİLMEDİ AMA CALLBACK TETİKLENİYOR!")
            android.util.Log.e("PermissionCheckDialog", "❌ Bu durumda callback tetiklenmemeli!")
            return
        }
        
        hasTriggeredCallback = true
        android.util.Log.i("PermissionCheckDialog", "✅ CALLBACK TETİKLENİYOR VE DIALOG KAPATILIYOR")
        
        // Callback'ı tetikle
        onPermissionsGranted?.invoke()
        
        // Dialog'ı kapat
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isAdded && !isRemoving) {
                dismiss()
            }
        }, 100)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("PermissionCheckDialog", "Dialog destroy ediliyor")
    }
    
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        android.util.Log.d("PermissionCheckDialog", "Dialog dismiss edildi")
    }
    
    companion object {
        fun newInstance(onPermissionsGranted: () -> Unit): PermissionCheckDialog {
            return PermissionCheckDialog().apply {
                this.onPermissionsGranted = onPermissionsGranted
            }
        }
    }
}
