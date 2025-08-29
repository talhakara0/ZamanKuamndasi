package com.talhadev.zamankumandasi.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.data.model.ProductType
import com.talhadev.zamankumandasi.data.model.PurchaseProduct
import com.talhadev.zamankumandasi.databinding.ItemPurchaseProductBinding

class PurchaseProductAdapter(
    private val onProductClick: (PurchaseProduct) -> Unit
) : ListAdapter<PurchaseProduct, PurchaseProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemPurchaseProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemPurchaseProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: PurchaseProduct) {
            binding.apply {
                tvProductTitle.text = product.title
                tvProductDescription.text = product.description
                tvProductPrice.text = product.price
                
                // Popüler badge
                if (product.isPopular) {
                    tvPopularBadge.visibility = android.view.View.VISIBLE
                    cardProduct.strokeColor = ContextCompat.getColor(root.context, R.color.primary)
                    cardProduct.strokeWidth = 4
                } else {
                    tvPopularBadge.visibility = android.view.View.GONE
                    cardProduct.strokeColor = ContextCompat.getColor(root.context, R.color.card_stroke)
                    cardProduct.strokeWidth = 1
                }

                // Ürün türüne göre özel bilgiler
                when (product.productType) {
                    ProductType.PREMIUM_MONTHLY -> {
                        tvProductPeriod.text = "Aylık"
                        tvProductPeriod.setBackgroundColor(ContextCompat.getColor(root.context, R.color.monthly_bg))
                    }
                    ProductType.PREMIUM_YEARLY -> {
                        tvProductPeriod.text = "Yıllık - %60 Tasarruf"
                        tvProductPeriod.setBackgroundColor(ContextCompat.getColor(root.context, R.color.yearly_bg))
                    }
                    ProductType.PREMIUM_LIFETIME -> {
                        tvProductPeriod.text = "Yaşam Boyu"
                        tvProductPeriod.setBackgroundColor(ContextCompat.getColor(root.context, R.color.lifetime_bg))
                    }
                }

                // Özellikler listesi
                val featuresText = product.features.joinToString("\n") { "• $it" }
                tvProductFeatures.text = featuresText

                // İndirim bilgisi
                if (product.discount.isNotEmpty() && product.originalPrice.isNotEmpty()) {
                    tvOriginalPrice.visibility = android.view.View.VISIBLE
                    tvDiscount.visibility = android.view.View.VISIBLE
                    tvOriginalPrice.text = product.originalPrice
                    tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    tvDiscount.text = product.discount
                } else {
                    tvOriginalPrice.visibility = android.view.View.GONE
                    tvDiscount.visibility = android.view.View.GONE
                }

                // Tıklama olayı
                cardProduct.setOnClickListener {
                    onProductClick(product)
                }

                btnChoosePlan.setOnClickListener {
                    onProductClick(product)
                }
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<PurchaseProduct>() {
        override fun areItemsTheSame(oldItem: PurchaseProduct, newItem: PurchaseProduct): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PurchaseProduct, newItem: PurchaseProduct): Boolean {
            return oldItem == newItem
        }
    }
}
