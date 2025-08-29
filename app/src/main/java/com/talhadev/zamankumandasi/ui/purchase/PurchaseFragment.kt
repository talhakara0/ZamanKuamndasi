package com.talhadev.zamankumandasi.ui.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.databinding.FragmentPurchaseBinding
import com.talhadev.zamankumandasi.ui.adapter.PurchaseProductAdapter
import com.talhadev.zamankumandasi.ui.viewmodel.AuthViewModel
import com.talhadev.zamankumandasi.ui.viewmodel.PurchaseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PurchaseFragment : Fragment() {

    private var _binding: FragmentPurchaseBinding? = null
    private val binding get() = _binding!!

    private val purchaseViewModel: PurchaseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var productAdapter: PurchaseProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPurchaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupRecyclerView()
        observeData()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFreeTrial.setOnClickListener {
            // Ücretsiz deneme başlat (örnek olarak en popüler ürünü seç)
            val popularProduct = purchaseViewModel.getPopularProduct()
            popularProduct?.let { product ->
                purchaseViewModel.purchaseProduct(requireActivity(), product.id)
            }
        }

        binding.btnRestorePurchases.setOnClickListener {
            // Satın alınanları geri yükle
            Toast.makeText(requireContext(), "Satın alımlar kontrol ediliyor...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        productAdapter = PurchaseProductAdapter(
            onProductClick = { product ->
                purchaseViewModel.purchaseProduct(requireActivity(), product.id)
            }
        )

        binding.rvProducts.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeData() {
        // Ürünleri gözlemle
        viewLifecycleOwner.lifecycleScope.launch {
            purchaseViewModel.products.collect { products ->
                if (products.isNotEmpty()) {
                    // Ürünler yüklendi
                    productAdapter.submitList(products)
                    binding.progressBar.visibility = View.GONE
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.tvNoProducts.visibility = View.GONE
                } else {
                    // Ürünler henüz yüklenmedi veya boş
                    binding.rvProducts.visibility = View.GONE
                }
            }
        }

        // Bağlantı durumunu gözlemle
        viewLifecycleOwner.lifecycleScope.launch {
            purchaseViewModel.isConnected.collect { isConnected ->
                if (!isConnected) {
                    // Bağlantı yok - loading'i durdur ve hata göster
                    binding.progressBar.visibility = View.GONE
                    binding.tvNoProducts.visibility = View.VISIBLE
                    binding.tvNoProducts.text = "Satın alma servisi bağlanamadı"
                    binding.cardConnectionStatus.visibility = View.VISIBLE
                    binding.tvConnectionStatus.text = "Satın alma servisi bağlanamadı"
                } else {
                    binding.cardConnectionStatus.visibility = View.GONE
                    binding.tvNoProducts.text = "Premium paketler yüklenemedi"
                }
            }
        }

        // Satın alma durumunu gözlemle
        viewLifecycleOwner.lifecycleScope.launch {
            purchaseViewModel.purchaseState.collect { state ->
                binding.progressBarPurchase.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                state.error?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    purchaseViewModel.clearPurchaseState()
                }

                if (state.success) {
                    Toast.makeText(requireContext(), "Satın alma başarılı! Premium özelliklere erişebilirsiniz.", Toast.LENGTH_LONG).show()
                    // Premium durumunu güncelle
                    authViewModel.setPremiumForCurrentUser(true)
                    purchaseViewModel.clearPurchaseState()
                    findNavController().navigateUp()
                }
            }
        }

        // Premium durumunu gözlemle
        viewLifecycleOwner.lifecycleScope.launch {
            purchaseViewModel.isPremium.collect { isPremium ->
                if (isPremium) {
                    binding.cardPremiumStatus.visibility = View.VISIBLE
                    binding.tvAlreadyPremium.text = "✓ Premium aktif"
                    binding.btnFreeTrial.text = "Premium Aktif"
                    binding.btnFreeTrial.isEnabled = false
                } else {
                    binding.cardPremiumStatus.visibility = View.GONE
                    binding.btnFreeTrial.text = "Ücretsiz Deneme Başlat"
                    binding.btnFreeTrial.isEnabled = true
                }
            }
        }

        // Satın alınanları gözlemle
        viewLifecycleOwner.lifecycleScope.launch {
            purchaseViewModel.purchases.collect { purchases ->
                val activePurchases = purchases.filter { it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED }
                if (activePurchases.isNotEmpty()) {
                    binding.cardActivePurchases.visibility = View.VISIBLE
                    binding.tvActivePurchases.text = "Aktif satın alımlar: ${activePurchases.size}"
                } else {
                    binding.cardActivePurchases.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
