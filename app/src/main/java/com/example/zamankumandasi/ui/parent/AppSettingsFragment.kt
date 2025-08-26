package com.example.zamankumandasi.ui.parent

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.zamankumandasi.R
import com.example.zamankumandasi.databinding.FragmentAppSettingsBinding
import com.example.zamankumandasi.ui.adapter.AppListAdapter
import com.example.zamankumandasi.ui.viewmodel.AppUsageViewModel
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppSettingsFragment : Fragment() {

    private var _binding: FragmentAppSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val appUsageViewModel: AppUsageViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
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
        observeChildren()
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
        // Önce çocuk seçimi dialog'u göster
        showChildSelectionDialog { selectedChild ->
            // Çocuk seçildikten sonra limit dialog'u göster
            showLimitDialog(packageName, appName, selectedChild)
        }
    }
    
    private fun showChildSelectionDialog(onChildSelected: (String) -> Unit) {
        val children = authViewModel.children.value ?: emptyList()
        
        if (children.isEmpty()) {
            // Çocuk yoksa uyarı göster
            AlertDialog.Builder(requireContext())
                .setTitle("Uyarı")
                .setMessage("Henüz bağlı çocuk bulunmuyor. Önce çocuk ekleyin.")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }
        
        val childNames = children.map { it.email }.toTypedArray()
        val childIds = children.map { it.id }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Çocuk Seçin")
            .setItems(childNames) { _, which ->
                val selectedChildId = childIds[which]
                onChildSelected(selectedChildId)
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun showLimitDialog(packageName: String, appName: String, childId: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_set_app_limit, null)
        
        val tvAppName = dialogView.findViewById<TextView>(R.id.tvAppName)
        val etLimitHours = dialogView.findViewById<EditText>(R.id.etLimitHours)
        val etLimitMinutes = dialogView.findViewById<EditText>(R.id.etLimitMinutes)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        
        val selectedChild = authViewModel.children.value?.find { it.id == childId }
        tvAppName.text = "$appName için ${selectedChild?.email ?: "Bilinmeyen"} hesabına limit belirleniyor"
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val hours = etLimitHours.text.toString().toIntOrNull() ?: 0
            val minutes = etLimitMinutes.text.toString().toIntOrNull() ?: 0
            val totalMinutes = hours * 60 + minutes
            
            if (totalMinutes <= 0) {
                // Geçersiz limit uyarısı
                AlertDialog.Builder(requireContext())
                    .setTitle("Hata")
                    .setMessage("Lütfen geçerli bir süre limiti girin.")
                    .setPositiveButton("Tamam", null)
                    .show()
                return@setOnClickListener
            }
            
            // Ebeveyn ID'sini al
            val currentUser = authViewModel.currentUser.value
            if (currentUser?.userType == com.example.zamankumandasi.data.model.UserType.PARENT) {
                appUsageViewModel.setDailyLimitForChild(
                    childUserId = childId,
                    packageName = packageName,
                    appName = appName,
                    limitInMinutes = totalMinutes,
                    parentUserId = currentUser.id
                )
                dialog.dismiss()
            } else {
                // Ebeveyn değilse hata ver
                AlertDialog.Builder(requireContext())
                    .setTitle("Hata")
                    .setMessage("Sadece ebeveyn hesapları limit belirleyebilir.")
                    .setPositiveButton("Tamam", null)
                    .show()
            }
        }
        
        dialog.show()
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
                // Hata veya başarı mesajını göster
                AlertDialog.Builder(requireContext())
                    .setTitle(if (it.contains("başarıyla")) "Başarılı" else "Hata")
                    .setMessage(it)
                    .setPositiveButton("Tamam", null)
                    .show()
            }
        }
    }
    
    private fun observeChildren() {
        authViewModel.children.observe(viewLifecycleOwner) { children ->
            println("Çocuklar yüklendi: ${children.size} çocuk")
            children.forEach { child ->
                println("Çocuk: ${child.email} (ID: ${child.id})")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
