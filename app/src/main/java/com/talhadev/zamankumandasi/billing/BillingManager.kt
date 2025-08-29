package com.talhadev.zamankumandasi.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.talhadev.zamankumandasi.data.model.PurchaseProduct
import com.talhadev.zamankumandasi.data.model.ProductType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"
        
        // Test product IDs - Production'da gerçek product ID'lerle değiştirin
        const val PREMIUM_MONTHLY = "premium_monthly"
        const val PREMIUM_YEARLY = "premium_yearly" 
        const val PREMIUM_LIFETIME = "premium_lifetime"
    }

    private var billingClient: BillingClient? = null
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _products = MutableStateFlow<List<PurchaseProduct>>(emptyList())
    val products: StateFlow<List<PurchaseProduct>> = _products.asStateFlow()
    
    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    init {
        initializeBillingClient()
        // Test için fallback products ekle
        loadFallbackProducts()
    }

    private fun loadFallbackProducts() {
        // Google Play bağlantısı başarısız olursa test için fallback products
        val fallbackProducts = listOf(
            PurchaseProduct(
                id = PREMIUM_MONTHLY,
                title = "Premium Aylık",
                description = "Aylık premium üyelik",
                price = "₺29,99",
                features = listOf(
                    "Reklamların kaldırılması",
                    "Sınırsız çocuk hesabı",
                    "Gelişmiş uygulama kontrolü"
                ),
                productType = ProductType.PREMIUM_MONTHLY
            ),
            PurchaseProduct(
                id = PREMIUM_YEARLY,
                title = "Premium Yıllık",
                description = "Yıllık premium üyelik - %60 tasarruf",
                price = "₺89,99",
                originalPrice = "₺359,88",
                discount = "%60 İndirim",
                features = listOf(
                    "Reklamların kaldırılması",
                    "Sınırsız çocuk hesabı",
                    "Gelişmiş uygulama kontrolü",
                    "12 ay için %60 tasarruf"
                ),
                isPopular = true,
                productType = ProductType.PREMIUM_YEARLY
            ),
            PurchaseProduct(
                id = PREMIUM_LIFETIME,
                title = "Premium Yaşam Boyu",
                description = "Tek seferlik ödeme ile yaşam boyu erişim",
                price = "₺249,99",
                features = listOf(
                    "Reklamların kaldırılması",
                    "Sınırsız çocuk hesabı",
                    "Gelişmiş uygulama kontrolü",
                    "Tek seferlik ödeme",
                    "Yaşam boyu erişim"
                ),
                productType = ProductType.PREMIUM_LIFETIME
            )
        )
        
        // Eğer henüz Google Play'den products yüklenmediyse fallback'i kullan
        if (_products.value.isEmpty()) {
            _products.value = fallbackProducts
        }
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing client setup finished successfully")
            _isConnected.value = true
            queryProducts()
            queryPurchases()
        } else {
            Log.e(TAG, "Billing client setup failed: ${billingResult.debugMessage}")
            _isConnected.value = false
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.d(TAG, "Billing service disconnected")
        _isConnected.value = false
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_YEARLY)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val products = productDetailsList.map { productDetails ->
                    convertToAppProduct(productDetails)
                }
                _products.value = products
                Log.d(TAG, "Products queried successfully from Google Play: ${products.size}")
            } else {
                Log.e(TAG, "Failed to query products from Google Play: ${billingResult.debugMessage}")
                // Fallback products zaten init'te yüklendi, burada bir şey yapmamıza gerek yok
            }
        }
    }

    private fun convertToAppProduct(productDetails: ProductDetails): PurchaseProduct {
        val features = when (productDetails.productId) {
            PREMIUM_MONTHLY -> listOf(
                "Reklamların kaldırılması",
                "Sınırsız çocuk hesabı",
                "Gelişmiş uygulama kontrolü"
            )
            PREMIUM_YEARLY -> listOf(
                "Reklamların kaldırılması",
                "Sınırsız çocuk hesabı", 
                "Gelişmiş uygulama kontrolü",
                "12 ay için %60 tasarruf"
            )
            PREMIUM_LIFETIME -> listOf(
                "Reklamların kaldırılması",
                "Sınırsız çocuk hesabı",
                "Gelişmiş uygulama kontrolü",
                "Tek seferlik ödeme",
                "Yaşam boyu erişim"
            )
            else -> listOf()
        }

        val productType = when (productDetails.productId) {
            PREMIUM_MONTHLY -> ProductType.PREMIUM_MONTHLY
            PREMIUM_YEARLY -> ProductType.PREMIUM_YEARLY
            PREMIUM_LIFETIME -> ProductType.PREMIUM_LIFETIME
            else -> ProductType.PREMIUM_MONTHLY
        }

        return PurchaseProduct(
            id = productDetails.productId,
            title = productDetails.title.replace("(Zaman Kumandası)", "").trim(),
            description = productDetails.description,
            price = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
            features = features,
            isPopular = productDetails.productId == PREMIUM_YEARLY,
            productType = productType
        )
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _purchases.value = purchases
                Log.d(TAG, "Purchases queried: ${purchases.size}")
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val product = _products.value.find { it.id == productId }
        if (product == null) {
            Log.e(TAG, "Product not found: $productId")
            return
        }

        // ProductDetails'i bul
        billingClient?.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                ))
                .build()
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient?.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "Purchase successful: ${purchase.products}")
        
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Purchase'ı acknowledge et
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged")
                        queryPurchases() // Satın alınanları güncelle
                    }
                }
            }
        }
    }

    fun isPremiumPurchased(): Boolean {
        return _purchases.value.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            (purchase.products.contains(PREMIUM_MONTHLY) || 
             purchase.products.contains(PREMIUM_YEARLY) ||
             purchase.products.contains(PREMIUM_LIFETIME))
        }
    }

    fun getActivePremiumPurchase(): Purchase? {
        return _purchases.value.find { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            (purchase.products.contains(PREMIUM_MONTHLY) || 
             purchase.products.contains(PREMIUM_YEARLY) ||
             purchase.products.contains(PREMIUM_LIFETIME))
        }
    }

    fun disconnect() {
        billingClient?.endConnection()
        _isConnected.value = false
    }
}
