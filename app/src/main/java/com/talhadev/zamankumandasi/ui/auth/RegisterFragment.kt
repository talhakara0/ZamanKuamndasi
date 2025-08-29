package com.talhadev.zamankumandasi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.data.model.User
import com.talhadev.zamankumandasi.data.model.UserType
import com.talhadev.zamankumandasi.databinding.FragmentRegisterBinding
import com.talhadev.zamankumandasi.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        observeAuthState()
    }

    private fun setupViews() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val userType = if (binding.rbParent.isChecked) UserType.PARENT else UserType.CHILD
            
            if (validateInputs(email, password, confirmPassword)) {
                authViewModel.signUp(email, password, userType)
            }
        }
        
        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateInputs(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = "E-posta gerekli"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Geçerli bir e-posta adresi girin"
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
        
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Şifre tekrarı gerekli"
            return false
        }
        
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Şifreler eşleşmiyor"
            return false
        }
        
        return true
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                }
                
                is AuthViewModel.AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    
                    if (state.user.userType == UserType.PARENT) {
                        Toast.makeText(context, 
                            "Eşleştirme kodunuz: ${state.user.pairingCode}", 
                            Toast.LENGTH_LONG).show()
                    }
                    
                    navigateBasedOnUserType(state.user)
                }
                
                is AuthViewModel.AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                
                is AuthViewModel.AuthState.SignedOut -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                }
            }
        }
    }

    private fun navigateBasedOnUserType(user: User) {
        when (user.userType) {
            UserType.PARENT -> {
                findNavController().navigate(R.id.action_registerFragment_to_parentDashboardFragment)
            }
            UserType.CHILD -> {
                findNavController().navigate(R.id.action_registerFragment_to_pairingFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
