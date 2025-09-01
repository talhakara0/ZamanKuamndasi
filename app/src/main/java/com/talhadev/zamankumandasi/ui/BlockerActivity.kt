package com.talhadev.zamankumandasi.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.talhadev.zamankumandasi.databinding.ActivityBlockerBinding

class BlockerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Bu uygulama"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Günlük süre sınırı aşıldı"

        binding.tvTitle.text = "$appName engellendi"
        binding.tvReason.text = reason

        // SÜPER GÜÇLÜ OTOMATİK ANA EKRANA DÖNÜŞ (1 saniye sonra)
        binding.btnHome.postDelayed({
            android.util.Log.d("BlockerActivity", "🚀 OTOMATİK ANA EKRANA DÖNÜŞ BAŞLIYOR!")
            
            // 5 kez ana ekrana dön
            repeat(5) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                startActivity(homeIntent)
                Thread.sleep(100) // 0.1 saniye bekle
            }
            
            android.util.Log.d("BlockerActivity", "✅ OTOMATİK ANA EKRANA DÖNÜŞ TAMAMLANDI!")
            finish()
        }, 1000) // 1 saniye sonra

        binding.btnHome.setOnClickListener {
            android.util.Log.d("BlockerActivity", "🔘 ANA EKRANA DÖN BUTONU TIKLANDI!")
            
            // SÜPER GÜÇLÜ ANA EKRANA DÖNÜŞ
            repeat(5) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                startActivity(homeIntent)
                Thread.sleep(100) // 0.1 saniye bekle
            }
            finish()
        }

        binding.btnAskParent.setOnClickListener {
            // İsteğe bağlı: Ebeveyn izni/ayarlarına yönlendirme (şimdilik sadece Ayarlar'a)
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            startActivity(settingsIntent)
        }
    }

    override fun onBackPressed() {
        // Geri tuşunu etkisiz bırak, ana ekrana yönlendir
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Bazı cihazlarda geri tuşu
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_REASON = "extra_reason"
    }
}
