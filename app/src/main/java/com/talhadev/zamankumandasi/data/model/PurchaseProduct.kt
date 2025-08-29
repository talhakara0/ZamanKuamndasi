package com.talhadev.zamankumandasi.data.model

data class PurchaseProduct(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val originalPrice: String = "",
    val discount: String = "",
    val features: List<String>,
    val isPopular: Boolean = false,
    val productType: ProductType = ProductType.PREMIUM_MONTHLY
)

enum class ProductType {
    PREMIUM_MONTHLY,
    PREMIUM_YEARLY
}

data class PurchaseState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val purchasedProduct: PurchaseProduct? = null
)
