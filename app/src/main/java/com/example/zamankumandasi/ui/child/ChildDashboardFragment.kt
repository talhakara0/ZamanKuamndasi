package com.example.zamankumandasi.ui.child

import android.app.usage.UsageEvents
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
import android.widget.Toast
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
import android.util.Log
import com.example.zamankumandasi.data.model.AppUsage

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

        if (!hasUsageStatsPermission(requireContext())) {
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(
                requireContext(),
                "Uygulama kullanım izni vermelisiniz!",
                Toast.LENGTH_LONG
            ).show()
        } else {
            startAppUsageService()
        }
    }

    private fun setupViews() {
        // Refresh butonu
        binding.btnRefresh.setOnClickListener {
            checkAndLoadUsageData()
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

                // Manuel kontrol
                checkAndLoadUsageData()
            }
        }
    }

    /**
     * QueryEvents tabanlı kullanım ölçümü
     */
    private fun checkAndLoadUsageData() {
        val usageStatsManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000 // Son 24 saat

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val usageMap = mutableMapOf<String, Long>()
        var lastEventTimeMap = mutableMapOf<String, Long>()

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)

            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastEventTimeMap[event.packageName] = event.timeStamp
            } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                val start = lastEventTimeMap[event.packageName]
                if (start != null) {
                    val duration = (event.timeStamp - start) / 1000
                    usageMap[event.packageName] = usageMap.getOrDefault(event.packageName, 0) + duration
                    lastEventTimeMap.remove(event.packageName)
                }
            }
        }

        Log.d("talha", "Toplam event sayısı: ${usageMap.size}")

        // 🔹 usageMap’i doğrudan adapter’e gönderiyoruz
        val usageList = usageMap.map { (pkg, dur) ->
            AppUsage(
                appName = getAppNameFromPackage(requireContext(), pkg),
                packageName = pkg,
                usedTime = dur
            )
        }

        // Kullanım süresine göre azalan şekilde sırala
        val sortedUsageList = usageList.sortedByDescending { it.usedTime }

        // Ana thread’de adapter’e bas
        activity?.runOnUiThread {
            appUsageAdapter.submitList(sortedUsageList)
        }
    }

    // 🔹 Paket adından uygulama adını alma fonksiyonu
    private fun getAppNameFromPackage(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun observeAppUsage() {
        appUsageViewModel.appUsageList.observe(viewLifecycleOwner) { usageList ->
            if (usageList.isNotEmpty()) {
                binding.tvNoUsage.visibility = View.GONE
                appUsageAdapter.submitList(usageList)
                Log.d("talha", "UI'ya ${usageList.size} uygulama yüklendi")
            } else {
                binding.tvNoUsage.visibility = View.VISIBLE
                Log.d("talha", "UI'da kullanım verisi yok")
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
        val usageStatsManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - (24 * 60 * 60 * 1000)

        try {
            val queryUsageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                currentTime
            )

            Log.d("talha", "Usage Access kontrolü: ${queryUsageStats.size} uygulama bulundu")

            if (queryUsageStats.isEmpty()) {
                val intent = Intent(requireContext(), UsageAccessActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(requireContext(), AppUsageService::class.java).apply {
                    action = "START_TRACKING"
                }
                requireContext().startService(intent)

                authViewModel.currentUser.value?.let { user ->
                    checkAndLoadUsageData()
                }
            }
        } catch (e: Exception) {
            Log.d("talha", "Usage Access hatası: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}
