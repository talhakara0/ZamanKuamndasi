package com.talhadev.zamankumandasi.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talhadev.zamankumandasi.billing.BillingManager
import com.talhadev.zamankumandasi.data.model.PurchaseProduct
import com.talhadev.zamankumandasi.data.model.PurchaseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    private val _purchaseState = MutableStateFlow(PurchaseState())
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    val products = billingManager.products
    val isConnected = billingManager.isConnected
    val purchases = billingManager.purchases

    // Premium durumunu kontrol et
    val isPremium = combine(purchases, isConnected) { purchaseList, connected ->
        connected && billingManager.isPremiumPurchased()
    }

    init {
        // BillingClient'ın bağlantı durumunu izle
        viewModelScope.launch {
            isConnected.collect { connected ->
                if (!connected) {
                    _purchaseState.value = _purchaseState.value.copy(
                        error = "Satın alma servisi bağlanamadı. Lütfen internet bağlantınızı kontrol edin."
                    )
                }
            }
        }
    }

    fun purchaseProduct(activity: Activity, productId: String) {
        _purchaseState.value = _purchaseState.value.copy(
            isLoading = true,
            error = null
        )

        try {
            billingManager.launchPurchaseFlow(activity, productId)
        } catch (e: Exception) {
            _purchaseState.value = _purchaseState.value.copy(
                isLoading = false,
                error = "Satın alma başlatılamadı: ${e.message}"
            )
        }
    }

    fun clearPurchaseState() {
        _purchaseState.value = PurchaseState()
    }

    fun getProductById(productId: String): PurchaseProduct? {
        return products.value.find { it.id == productId }
    }

    fun getPopularProduct(): PurchaseProduct? {
        return products.value.find { it.isPopular }
    }

    fun getMostExpensiveProduct(): PurchaseProduct? {
        return products.value.maxByOrNull { 
            it.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0 
        }
    }

    fun getCheapestProduct(): PurchaseProduct? {
        return products.value.minByOrNull { 
            it.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: Double.MAX_VALUE 
        }
    }

    override fun onCleared() {
        super.onCleared()
        // BillingManager disconnect etme, singleton olduğu için diğer yerler de kullanabilir
    }
}
