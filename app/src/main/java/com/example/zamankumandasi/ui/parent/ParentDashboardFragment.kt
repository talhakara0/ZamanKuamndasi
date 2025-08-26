package com.example.zamankumandasi.ui.parent

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
import com.example.zamankumandasi.utils.SimpleLogout
import com.example.zamankumandasi.data.model.UserType
import com.example.zamankumandasi.databinding.FragmentParentDashboardBinding
import com.example.zamankumandasi.ui.adapter.ChildrenAdapter
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParentDashboardFragment : Fragment() {

    private var _binding: FragmentParentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var childrenAdapter: ChildrenAdapter
    
    // LogoutManager removed; using SimpleLogout utility

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Fragment'teki toolbar menüsünü aktif et
        val toolbar = binding.toolbar
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    android.util.Log.d("talha", "ParentDashboard: Toolbar çıkış menu item seçildi")
                    SimpleLogout.confirmAndSignOut(this, authViewModel)
                    true
                }
                R.id.action_toggle_premium -> {
                    val isPremium = authViewModel.currentUser.value?.isPremium == true
                    authViewModel.setPremiumForCurrentUser(!isPremium)
                    true
                }
                else -> false
            }
        }
        
        setupViews()
        setupRecyclerView()
        observeCurrentUser()
    observeAuthState()
        observeChildren()
    }

    private fun setupViews() {
        binding.btnManageApps.setOnClickListener {
            findNavController().navigate(R.id.action_parentDashboardFragment_to_appSettingsFragment)
        }
        
        binding.btnViewUsage.setOnClickListener {
            // TODO: Kullanım raporları ekranına git
        }
    }

    private fun setupRecyclerView() {
        childrenAdapter = ChildrenAdapter(
            onChildClick = { child ->
                // Çocuk detayına git (kullanım verisi)
                // Premium değilse arada reklam göster
                com.example.zamankumandasi.ads.AdManager.maybeShowInterstitial(requireActivity())
                val bundle = android.os.Bundle().apply {
                    putString("childId", child.id)
                    putString("childEmail", child.email)
                }
                findNavController().navigate(R.id.action_parentDashboardFragment_to_childUsageDetailFragment, bundle)
            },
            onManageAppsClick = { child ->
                // Çocuğun uygulamalarını yönet
            },
            onViewUsageClick = { child ->
                // Çocuğun kullanım verilerini görüntüle
            }
        )

        binding.rvChildren.apply {
            adapter = childrenAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeCurrentUser() {
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Premium reklam durumu
                com.example.zamankumandasi.ads.AdManager.setPremium(it.isPremium)
                binding.tvUserEmail.text = it.email
                binding.tvPairingCode.text = "Eşleştirme Kodu: ${it.pairingCode}"
                android.util.Log.d("talha", "Ebeveyn id: ${it.id}, email: ${it.email}")
                // Eğer ebeveyn ise çocuklarını yükle
                if (it.userType == UserType.PARENT) {
                    authViewModel.loadChildren(it.id)
                }
                // Menü başlığını güncelle (dev toggle)
                val item = binding.toolbar.menu.findItem(R.id.action_toggle_premium)
                item?.title = if (it.isPremium) "Premium'u kapat (dev)" else "Premium'a geç (dev)"
            }
        }
    }
    
    private fun observeChildren() {
        authViewModel.children.observe(viewLifecycleOwner) { children ->
            android.util.Log.d("talha", "Yüklenen çocuk sayısı: ${children.size}")
            children.forEach { child ->
                android.util.Log.d("talha", "Çocuk: ${child.email}, parentId: ${child.parentId}")
            }
            if (children.isNotEmpty()) {
                binding.tvNoChildren.visibility = View.GONE
                childrenAdapter.submitList(children)
            } else {
                binding.tvNoChildren.visibility = View.VISIBLE
            }
        }
    }

    // Eski deprecated menü fonksiyonları kaldırıldı - modern MenuProvider kullanılıyor

    private fun performDirectLogoutParent() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Çıkış Yap")
            .setMessage("Çıkış yapmak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { dialog, _ ->
                dialog.dismiss()
                authViewModel.signOut()
            }
            .setNegativeButton("Hayır") { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.SignedOut -> {
                    try {
                        val navOptions = androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .setLaunchSingleTop(true)
                            .build()
                        findNavController().navigate(R.id.loginFragment, null, navOptions)
                        Toast.makeText(context, "Başarıyla çıkış yapıldı", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Çıkış sonrası yönlendirme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                is AuthViewModel.AuthState.Error -> {
                    Toast.makeText(context, "Çıkış hatası: ${state.message}", Toast.LENGTH_LONG).show()
                }
                else -> {
                    // ignore loading/success
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
