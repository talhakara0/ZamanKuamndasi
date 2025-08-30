package com.talhadev.zamankumandasi.ui.parent

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
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.utils.SimpleLogout
import com.talhadev.zamankumandasi.data.model.UserType
import com.talhadev.zamankumandasi.databinding.FragmentParentDashboardBinding
import com.talhadev.zamankumandasi.ui.adapter.ChildrenAdapter
import com.talhadev.zamankumandasi.ui.viewmodel.AuthViewModel
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
                R.id.action_premium -> {
                    findNavController().navigate(R.id.action_parentDashboardFragment_to_purchaseFragment)
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
                if (authViewModel.currentUser.value?.isPremium != true) {
                    com.talhadev.zamankumandasi.ads.AdManager.maybeShowInterstitial(requireActivity())
                }
                val bundle = android.os.Bundle().apply {
                    putString("childId", child.id)
                    putString("childEmail", child.email)
                }
                findNavController().navigate(R.id.action_parentDashboardFragment_to_childUsageDetailFragment, bundle)
            },
            onManageAppsClick = { child ->
                // Çocuğun kullanım verilerini görüntüle - ChildUsageDetailFragment'a git
                val bundle = android.os.Bundle().apply {
                    putString("childId", child.id)
                    putString("childEmail", child.email)
                }
                findNavController().navigate(R.id.action_parentDashboardFragment_to_childUsageDetailFragment, bundle)
            },
            onViewUsageClick = { child ->
                // Çocuğun uygulamalarını görüntüle - AppSettingsFragment'a git
                val bundle = android.os.Bundle().apply {
                    putString("childId", child.id)
                    putString("childName", if (child.name.isNotEmpty()) child.name else "İsimsiz Çocuk")
                    putString("childEmail", child.email)
                }
                findNavController().navigate(R.id.action_parentDashboardFragment_to_appSettingsFragment, bundle)
            },
            onEditNameClick = { child ->
                showEditChildNameDialog(child)
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
                com.talhadev.zamankumandasi.ads.AdManager.setPremium(it.isPremium)
                binding.tvUserEmail.text = it.email
                binding.tvPairingCode.text = "Eşleştirme Kodu: ${it.pairingCode}"
                android.util.Log.d("talha", "Ebeveyn id: ${it.id}, email: ${it.email}")
                // Eğer ebeveyn ise çocuklarını yükle
                if (it.userType == UserType.PARENT) {
                    authViewModel.loadChildren(it.id)
                }
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

    private fun showEditChildNameDialog(child: com.talhadev.zamankumandasi.data.model.User) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.talhadev.zamankumandasi.R.layout.dialog_edit_child_name, null
        )
        val etChildName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.talhadev.zamankumandasi.R.id.etChildName
        )
        
        // Mevcut ismi set et
        etChildName.setText(child.name)
        
        // Emülatörde Türkçe karakter desteği için input type'ı ayarla
        etChildName.inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                               android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME or
                               android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        
        // Emülatörde Türkçe karakter desteği için ek ayarlar
        etChildName.isLongClickable = false
        etChildName.setTextIsSelectable(true)
        
        val alertDialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Kaydet") { dialog, _ ->
                val newName = etChildName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Türkçe karakterleri koru ve ilk harfi büyük yap
                    val formattedName = newName.replaceFirstChar { 
                        if (it.isLowerCase()) it.uppercase() else it.toString() 
                    }
                    
                    // Sadece harf ve boşluk karakterlerine izin ver (Türkçe karakterler dahil)
                    val cleanName = formattedName.replace(Regex("[^a-zA-ZçğıöşüÇĞIİÖŞÜ ]"), "")
                        .replace(Regex("\\s+"), " ") // Birden fazla boşluğu tek boşluğa çevir
                        .trim()
                    
                    if (cleanName.isNotEmpty()) {
                        authViewModel.updateChildName(child.id, cleanName)
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Çocuk ismi güncellendi: $cleanName", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Geçerli bir isim giriniz", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "İsim boş olamaz", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        alertDialog.show()
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
