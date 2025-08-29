package com.talhadev.zamankumandasi.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.talhadev.zamankumandasi.data.model.User
import com.talhadev.zamankumandasi.databinding.ItemChildBinding

class ChildrenAdapter(
    private val onChildClick: (User) -> Unit,
    private val onManageAppsClick: (User) -> Unit,
    private val onViewUsageClick: (User) -> Unit
) : ListAdapter<User, ChildrenAdapter.ChildViewHolder>(ChildDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val binding = ItemChildBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChildViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChildViewHolder(
        private val binding: ItemChildBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(child: User) {
            binding.apply {
                tvChildEmail.text = child.email
                tvDeviceId.text = "Cihaz ID: ${child.deviceId ?: "Bilinmiyor"}"
                
                // Eşleştirme zamanını formatla
                val createdAtFormatted = if (child.createdAt > 0) {
                    val date = java.util.Date(child.createdAt)
                    java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(date)
                } else {
                    "Bilinmiyor"
                }
                tvCreatedAt.text = "Eşleştirildi: $createdAtFormatted"
                
                btnManageApps.setOnClickListener {
                    onManageAppsClick(child)
                }
                
                btnViewUsage.setOnClickListener {
                    onViewUsageClick(child)
                }
                
                root.setOnClickListener {
                    onChildClick(child)
                }
            }
        }
    }

    private class ChildDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
