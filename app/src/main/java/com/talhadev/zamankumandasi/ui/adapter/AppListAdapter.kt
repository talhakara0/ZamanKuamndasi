package com.talhadev.zamankumandasi.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.talhadev.zamankumandasi.data.model.AppInfo
import com.talhadev.zamankumandasi.databinding.ItemAppListBinding

class AppListAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onSetLimitClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(
        private val binding: ItemAppListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.apply {
                tvAppName.text = app.appName
                tvPackageName.text = app.packageName
                // Uygulama ikonunu yükle (gerçek ikon)
                val pm = binding.root.context.packageManager
                try {
                    val icon = pm.getApplicationIcon(app.packageName)
                    ivAppIcon.setImageDrawable(icon)
                } catch (e: Exception) {
                    ivAppIcon.setImageResource(android.R.drawable.ic_menu_manage)
                }
                btnSetLimit.setOnClickListener {
                    onSetLimitClick(app)
                }
                root.setOnClickListener {
                    onAppClick(app)
                }
            }
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}
