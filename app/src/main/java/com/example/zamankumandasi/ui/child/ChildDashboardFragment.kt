package com.example.zamankumandasi.ui.child

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.zamankumandasi.R
import com.example.zamankumandasi.databinding.FragmentChildDashboardBinding
import com.example.zamankumandasi.service.AppUsageService
import com.example.zamankumandasi.ui.UsageAccessActivity
import com.example.zamankumandasi.ui.adapter.AppUsageAdapter
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel
import com.example.zamankumandasi.ui.viewmodel.AppUsageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChildDashboardFragment : Fragment() {

    private var _binding: FragmentChildDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()
    private val appUsageViewModel: AppUsageViewModel by viewModels()
    private lateinit var appUsageAdapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setHasOptionsMenu(true)
        setupViews()
        setupRecyclerView()
        observeCurrentUser()
        observeAppUsage()
        startAppUsageService()
    }

    private fun setupViews() {
        // Refresh butonu
        binding.btnRefresh.setOnClickListener {
            authViewModel.currentUser.value?.let { user ->
                loadUsageDataManually(user.id)
            }
        }
    }

    private fun setupRecyclerView() {
        appUsageAdapter = AppUsageAdapter(
            onAppClick = { appUsage ->
                // Uygulama kullanım detayına git
            }
        )

        binding.rvAppUsage.apply {
            adapter = appUsageAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeCurrentUser() {
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvUserEmail.text = it.email
                binding.tvParentInfo.text = "Ebeveyn ID: ${it.parentId ?: "Henüz eşleştirilmedi"}"
                
                // Kullanım verilerini yükle
                appUsageViewModel.loadAppUsageByUser(it.id)
                
                // Manuel olarak da kullanım verilerini kontrol et
                checkAndLoadUsageData(it.id)
            }
        }
    }
    
    private fun checkAndLoadUsageData(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Kullanım verilerini manuel olarak kontrol et
                val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (24 * 60 * 60 * 1000) // Son 24 saat
                
                val usageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                )
                
                println("checkAndLoadUsageData: ${usageStats.size} uygulama bulundu")
                
                // Kullanım verilerini güncelle
                usageStats.forEach { stats ->
                    if (stats.totalTimeInForeground > 0) {
                        println("${stats.packageName}: ${stats.totalTimeInForeground}ms")
                        appUsageViewModel.updateUsedTime(userId, stats.packageName, stats.totalTimeInForeground)
                    }
                }
                
                // Test verisi ekle (debug için)
                if (usageStats.isEmpty()) {
                    addTestData(userId)
                }
                
                // Listeyi yenile
                appUsageViewModel.loadAppUsageByUser(userId)
                
            } catch (e: Exception) {
                println("checkAndLoadUsageData hatası: ${e.message}")
                e.printStackTrace()
                
                // Hata durumunda test verisi ekle
                addTestData(userId)
            }
        }
    }
    
    private fun addTestData(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Test verileri ekle
                appUsageViewModel.updateUsedTime(userId, "com.whatsapp", 300000) // 5 dakika
                appUsageViewModel.updateUsedTime(userId, "com.instagram.android", 600000) // 10 dakika
                appUsageViewModel.updateUsedTime(userId, "com.google.android.youtube", 900000) // 15 dakika
                
                println("Test verileri eklendi")
                
                // Listeyi yenile
                appUsageViewModel.loadAppUsageByUser(userId)
                
            } catch (e: Exception) {
                println("Test verisi ekleme hatası: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun observeAppUsage() {
        appUsageViewModel.appUsageList.observe(viewLifecycleOwner) { usageList ->
            if (usageList.isNotEmpty()) {
                binding.tvNoUsage.visibility = View.GONE
                appUsageAdapter.submitList(usageList)
                // Debug için log
                println("Kullanım verileri yüklendi: ${usageList.size} uygulama")
                usageList.forEach { usage ->
                    println("${usage.appName}: ${usage.usedTime}ms")
                }
            } else {
                binding.tvNoUsage.visibility = View.VISIBLE
                println("Kullanım verisi bulunamadı")
            }
        }
        
        appUsageViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_child_dashboard, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                authViewModel.signOut()
                findNavController().navigate(R.id.action_childDashboardFragment_to_loginFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun startAppUsageService() {
        // Usage Access izni kontrolü - daha detaylı kontrol
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - (24 * 60 * 60 * 1000) // Son 24 saat
        
        try {
            val queryUsageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, 
                startTime, 
                currentTime
            )
            
            println("Usage Access kontrolü: ${queryUsageStats.size} uygulama bulundu")
            
            if (queryUsageStats.isEmpty()) {
                // Usage Access izni yok, kullanıcıyı yönlendir
                val intent = Intent(requireContext(), UsageAccessActivity::class.java)
                startActivity(intent)
            } else {
                // Service'i başlat
                val intent = Intent(requireContext(), AppUsageService::class.java).apply {
                    action = "START_TRACKING"
                }
                requireContext().startService(intent)
                
                // Manuel olarak da verileri yükle
                authViewModel.currentUser.value?.let { user ->
                    loadUsageDataManually(user.id)
                }
            }
        } catch (e: Exception) {
            println("Usage Access hatası: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun loadUsageDataManually(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val currentTime = System.currentTimeMillis()
                val startTime = currentTime - (24 * 60 * 60 * 1000) // Son 24 saat
                
                val usageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    currentTime
                )
                
                println("Manuel yükleme: ${usageStats.size} uygulama bulundu")
                
                usageStats.forEach { stats ->
                    if (stats.totalTimeInForeground > 0) {
                        println("${stats.packageName}: ${stats.totalTimeInForeground}ms")
                        appUsageViewModel.updateUsedTime(userId, stats.packageName, stats.totalTimeInForeground)
                    }
                }
                
                // Listeyi yenile
                appUsageViewModel.loadAppUsageByUser(userId)
                
            } catch (e: Exception) {
                println("Manuel yükleme hatası: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
