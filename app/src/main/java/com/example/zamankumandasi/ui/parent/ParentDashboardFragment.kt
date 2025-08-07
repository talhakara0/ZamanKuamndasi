package com.example.zamankumandasi.ui.parent

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
        
        setHasOptionsMenu(true)
        setupViews()
        setupRecyclerView()
        observeCurrentUser()
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
                // Çocuk detayına git
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
                binding.tvUserEmail.text = it.email
                binding.tvPairingCode.text = "Eşleştirme Kodu: ${it.pairingCode}"
                
                // Eğer ebeveyn ise çocuklarını yükle
                if (it.userType == UserType.PARENT) {
                    authViewModel.loadChildren(it.id)
                }
            }
        }
    }
    
    private fun observeChildren() {
        authViewModel.children.observe(viewLifecycleOwner) { children ->
            if (children.isNotEmpty()) {
                binding.tvNoChildren.visibility = View.GONE
                childrenAdapter.submitList(children)
                println("Çocuklar yüklendi: ${children.size} çocuk")
                children.forEach { child ->
                    println("Çocuk: ${child.email}")
                }
            } else {
                binding.tvNoChildren.visibility = View.VISIBLE
                println("Çocuk bulunamadı")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_parent_dashboard, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                authViewModel.signOut()
                findNavController().navigate(R.id.action_parentDashboardFragment_to_loginFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
