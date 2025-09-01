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
        return Dialog(requireContext(), R.style.FullScreenDialog)
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
        
        permissionStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
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
        
        // Overlay izni (layout'ta "Accessibility" olarak tanımlı)
        val tvAccessibility = view?.findViewById<TextView>(R.id.tvAccessibility)
        val ivAccessibility = view?.findViewById<ImageView>(R.id.ivAccessibility)
        val btnAccessibility = view?.findViewById<Button>(R.id.btnAccessibility)
        
        tvAccessibility?.text = if (status.overlay) {
            "✅ Overlay izni verildi"
        } else {
            "❌ Diğer uygulamalar üstünde gösterme izni gerekli"
        }
        
        ivAccessibility?.setImageResource(
            if (status.overlay) android.R.drawable.ic_menu_send else android.R.drawable.ic_menu_close_clear_cancel
        )
        
        btnAccessibility?.setOnClickListener {
            PermissionHelper.openOverlaySettings(requireContext())
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
        btnContinue?.visibility = if (status.allGranted) View.VISIBLE else View.GONE
        btnContinue?.setOnClickListener {
            android.util.Log.d("PermissionCheckDialog", "Devam et butonuna tıklandı")
            triggerCallbackAndDismiss()
        }
        
        // Açıklama metni
        val tvDescription = view?.findViewById<TextView>(R.id.tvDescription)
        tvDescription?.text = if (status.allGranted) {
            "Tüm temel izinler verildi! Uygulama engelleme sistemi çalışmaya hazır."
        } else {
            "Uygulama engelleme sisteminin çalışması için aşağıdaki temel izinlerin verilmesi gerekiyor:"
        }
        
        // Yardım metinleri
        if (!status.overlay) {
            val tvAccessibilityHelp = view?.findViewById<TextView>(R.id.tvAccessibilityHelp)
            tvAccessibilityHelp?.visibility = View.VISIBLE
            tvAccessibilityHelp?.text = "💡 Yardım: Overlay izni verilemiyorsa:\n" +
                "1. Ayarlar > Uygulamalar > ZamanKumandasi'ne gidin\n" +
                "2. 'Diğer uygulamalar üzerinde görüntüle' seçeneğini bulun\n" +
                "3. Switch'i açın\n" +
                "4. Bu izin sayesinde limit dolduğunda uyarı gösterebiliriz\n\n" +
                "🔑 Not: Erişilebilirlik servisi opsiyoneldir ve sonra etkinleştirilebilir."
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
            android.util.Log.d("PermissionCheckDialog", "Callback zaten tetiklendi - refresh atlanıyor")
            return
        }
        
        android.util.Log.d("PermissionCheckDialog", "İzinler yenileniyor...")
        permissionStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
        
        // Yeni permission status'u kontrol et
        val newStatus = permissionStatus
        if (newStatus != null) {
            android.util.Log.d("PermissionCheckDialog", "Yeni izin durumu: Usage=${newStatus.usageStats}, Overlay=${newStatus.overlay}, Accessibility=${newStatus.accessibility}, Battery=${newStatus.batteryOptimization}, AllGranted=${newStatus.allGranted}")
        }
        
        setupUI()
        
        // Eğer tüm izinler verildiyse otomatik olarak devam et
        if (permissionStatus?.allGranted == true) {
            android.util.Log.d("PermissionCheckDialog", "Refresh sonrası tüm izinler mevcut - devam ediliyor")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                triggerCallbackAndDismiss()
            }, 400) // 400ms bekle
        } else {
            android.util.Log.d("PermissionCheckDialog", "Refresh sonrası hala eksik izinler var: ${permissionStatus}")
        }
    }
    
    private fun triggerCallbackAndDismiss() {
        if (hasTriggeredCallback) {
            android.util.Log.d("PermissionCheckDialog", "Callback zaten tetiklendi - tekrar tetiklenmiyor")
            return
        }
        
        if (!isAdded || isRemoving) {
            android.util.Log.d("PermissionCheckDialog", "Dialog durumu uygun değil - callback atlanıyor")
            return
        }
        
        hasTriggeredCallback = true
        android.util.Log.d("PermissionCheckDialog", "Callback tetikleniyor ve dialog kapatılıyor")
        
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
