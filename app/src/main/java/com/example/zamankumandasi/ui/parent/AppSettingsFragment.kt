package com.example.zamankumandasi.ui.parent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.zamankumandasi.databinding.FragmentAppSettingsBinding
import com.example.zamankumandasi.ui.adapter.AppListAdapter
import com.example.zamankumandasi.ui.viewmodel.AppUsageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppSettingsFragment : Fragment() {

    private var _binding: FragmentAppSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val appUsageViewModel: AppUsageViewModel by viewModels()
    private lateinit var appListAdapter: AppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupRecyclerView()
        observeAppUsage()
        loadInstalledApps()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        appListAdapter = AppListAdapter(
            onAppClick = { app ->
                // Uygulama detayına git
            },
            onSetLimitClick = { app ->
                showSetLimitDialog(app.packageName, app.appName)
            }
        )

        binding.rvApps.apply {
            adapter = appListAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun showSetLimitDialog(packageName: String, appName: String) {
        // TODO: Süre belirleme dialog'u göster
        // appUsageViewModel.setDailyLimit(packageName, limitInMillis)
    }

    private fun loadInstalledApps() {
        appUsageViewModel.loadInstalledApps()
    }

    private fun observeAppUsage() {
        appUsageViewModel.installedApps.observe(viewLifecycleOwner) { apps ->
            if (apps.isNotEmpty()) {
                binding.tvNoApps.visibility = View.GONE
                appListAdapter.submitList(apps)
            } else {
                binding.tvNoApps.visibility = View.VISIBLE
            }
        }
        
        appUsageViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        appUsageViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // TODO: Hata mesajını göster
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
