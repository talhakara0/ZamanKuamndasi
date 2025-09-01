package com.talhadev.zamankumandasi.ui

import android.app.Dialog
import android.content.Context
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
        val tvAccessibility = view?.findViewById<TextView>(R.id.tvAccessibility)
        val ivAccessibility = view?.findViewById<ImageView>(R.id.ivAccessibility)
        val btnAccessibility = view?.findViewById<Button>(R.id.btnAccessibility)
        
        tvAccessibility?.text = if (status.accessibility) {
            "✅ Overlay izni verildi"
        } else {
            "❌ Overlay izni gerekli"
        }
        
        ivAccessibility?.setImageResource(
            if (status.accessibility) android.R.drawable.ic_menu_send else android.R.drawable.ic_menu_close_clear_cancel
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
            onPermissionsGranted?.invoke()
            dismiss()
        }
        
        // Açıklama metni
        val tvDescription = view?.findViewById<TextView>(R.id.tvDescription)
        tvDescription?.text = if (status.allGranted) {
            "Tüm gerekli izinler verildi! Uygulama engelleme sistemi artık tam olarak çalışacak."
        } else {
            "Uygulama engelleme sisteminin çalışması için aşağıdaki izinlerin verilmesi gerekiyor:"
        }
        
        // Overlay izni için özel yardım
        if (!status.accessibility) {
            val tvAccessibilityHelp = view?.findViewById<TextView>(R.id.tvAccessibilityHelp)
            tvAccessibilityHelp?.visibility = View.VISIBLE
            tvAccessibilityHelp?.text = "💡 Yardım: Overlay izni verilemiyorsa:\n" +
                "1. Ayarlar > Uygulamalar > ZamanKumandasi'ne gidin\n" +
                "2. 'Diğer uygulamalar üzerinde görüntüle' seçeneğini bulun\n" +
                "3. Switch'i açın\n" +
                "4. Bu izin sayesinde limit dolduğunda uyarı gösterebiliriz"
        }
    }
    
    private fun refreshPermissions() {
        permissionStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
        setupUI()
    }
    
    companion object {
        fun newInstance(onPermissionsGranted: () -> Unit): PermissionCheckDialog {
            return PermissionCheckDialog().apply {
                this.onPermissionsGranted = onPermissionsGranted
            }
        }
    }
}
