package com.example.zamankumandasi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.zamankumandasi.R
import com.example.zamankumandasi.data.model.User
import com.example.zamankumandasi.data.model.UserType
import com.example.zamankumandasi.databinding.FragmentLoginBinding
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // MainActivity'de otomatik giriş kontrolü yapılıyor
        // Burada sadece UI setup yapıyoruz
        
        setupViews()
        observeAuthState()
    }

    private fun setupViews() {
        // Network kontrol disabled - internet varken yanlış uyarı vermesin
        // checkNetworkStatus()
        
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (validateInputs(email, password)) {
                authViewModel.signIn(email, password)
            }
        }
        
        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
    
    private fun checkNetworkStatus() {
        // Network kontrolü tamamen devre dışı - internet var kabul ediliyor
        android.util.Log.d("talha", "Network: ✅ İnternet kontrolü devre dışı (varsayılan: bağlı)")
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = "E-posta gerekli"
            return false
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Şifre gerekli"
            return false
        }
        
        if (password.length < 6) {
            binding.tilPassword.error = "Şifre en az 6 karakter olmalı"
            return false
        }
        
        return true
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                
                is AuthViewModel.AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    
                    // Sadece LoginFragment'tayken navigate et
                    if (findNavController().currentDestination?.id == R.id.loginFragment) {
                        navigateBasedOnUserType(state.user)
                    }
                }
                
                is AuthViewModel.AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                
                is AuthViewModel.AuthState.SignedOut -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                }
            }
        }
    }

    private fun navigateBasedOnUserType(user: User) {
        // Sadece LoginFragment'tayken navigate et
        if (findNavController().currentDestination?.id != R.id.loginFragment) {
            return
        }
        
        try {
            when (user.userType) {
                UserType.PARENT -> {
                    findNavController().navigate(R.id.action_loginFragment_to_parentDashboardFragment)
                }
                UserType.CHILD -> {
                    if (user.parentId == null) {
                        // Çocuk henüz ebeveynle eşleştirilmemiş
                        findNavController().navigate(R.id.action_loginFragment_to_pairingFragment)
                    } else {
                        findNavController().navigate(R.id.action_loginFragment_to_childDashboardFragment)
                    }
                }
            }
        } catch (e: Exception) {
            // Navigation hatası durumunda log at ve görmezden gel
            android.util.Log.e("talha", "Navigation error: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
