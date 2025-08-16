package com.example.zamankumandasi.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.zamankumandasi.data.model.AppUsage
import com.example.zamankumandasi.databinding.ItemAppUsageBinding
import java.text.SimpleDateFormat
import java.util.*

class AppUsageAdapter(
    private val onAppClick: (AppUsage) -> Unit
) : ListAdapter<AppUsage, AppUsageAdapter.AppUsageViewHolder>(AppUsageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppUsageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppUsageViewHolder(
        private val binding: ItemAppUsageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appUsage: AppUsage) {
            binding.apply {
                tvAppName.text = appUsage.appName
                tvPackageName.text = appUsage.packageName
                
                // Kullanım süresini formatla
                val usedTimeFormatted = formatTime(appUsage.usedTime)
                val dailyLimitFormatted = formatTime(appUsage.dailyLimit)
                
                tvUsedTime.text = "Kullanılan: $usedTimeFormatted"
                tvDailyLimit.text = "Günlük Limit: $dailyLimitFormatted"
                
                // Son kullanım zamanını formatla
                val lastUsedFormatted = if (appUsage.lastUsed > 0) {
                    val date = Date(appUsage.lastUsed)
                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(date)
                } else {
                    "Hiç kullanılmadı"
                }
                tvLastUsed.text = "Son Kullanım: $lastUsedFormatted"
                
                // Durum göstergesi
                if (appUsage.isBlocked) {
                    tvStatus.text = "ENGELENMİŞ"
                    tvStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
                } else {
                    val progress = if (appUsage.dailyLimit > 0) {
                        (appUsage.usedTime * 100 / appUsage.dailyLimit).toInt()
                    } else {
                        0
                    }
                    tvStatus.text = "%$progress Kullanıldı"
                    tvStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
                }
                
                // Uygulama ikonunu yükle (gerçek ikon)
                val pm = binding.root.context.packageManager
                try {
                    val icon = pm.getApplicationIcon(appUsage.packageName)
                    ivAppIcon.setImageDrawable(icon)
                } catch (e: Exception) {
                    ivAppIcon.setImageResource(android.R.drawable.ic_menu_manage)
                }
                
                root.setOnClickListener {
                    onAppClick(appUsage)
                }
            }
        }
        
        private fun formatTime(timeInMillis: Long): String {
            val hours = timeInMillis / (1000 * 60 * 60)
            val minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60)
            
            return when {
                hours > 0 -> "${hours}s ${minutes}dk"
                minutes > 0 -> "${minutes}dk"
                else -> "${timeInMillis / 1000}sn"
            }
        }
    }

    private class AppUsageDiffCallback : DiffUtil.ItemCallback<AppUsage>() {
        override fun areItemsTheSame(oldItem: AppUsage, newItem: AppUsage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppUsage, newItem: AppUsage): Boolean {
            return oldItem == newItem
        }
    }
}
