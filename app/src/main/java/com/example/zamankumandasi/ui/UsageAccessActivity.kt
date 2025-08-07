package com.example.zamankumandasi.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.zamankumandasi.databinding.ActivityUsageAccessBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsageAccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageAccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsageAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        checkUsageAccessPermission()
    }

    private fun setupViews() {
        binding.btnGrantPermission.setOnClickListener {
            openUsageAccessSettings()
        }

        binding.btnContinue.setOnClickListener {
            if (hasUsageAccessPermission()) {
                finish()
            } else {
                Toast.makeText(this, "Kullanım erişimi izni gerekli", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkUsageAccessPermission() {
        if (hasUsageAccessPermission()) {
            binding.tvStatus.text = "Kullanım erişimi izni verildi"
            binding.btnGrantPermission.visibility = android.view.View.GONE
            binding.btnContinue.visibility = android.view.View.VISIBLE
        } else {
            binding.tvStatus.text = "Kullanım erişimi izni gerekli"
            binding.btnGrantPermission.visibility = android.view.View.VISIBLE
            binding.btnContinue.visibility = android.view.View.GONE
        }
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        checkUsageAccessPermission()
    }
}
