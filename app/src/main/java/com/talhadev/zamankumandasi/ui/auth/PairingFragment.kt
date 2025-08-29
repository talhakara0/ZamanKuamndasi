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
import com.talhadev.zamankumandasi.databinding.FragmentPairingBinding
import com.talhadev.zamankumandasi.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PairingFragment : Fragment() {

    private var _binding: FragmentPairingBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPairingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        observeAuthState()
    }

    private fun setupViews() {
        binding.btnPair.setOnClickListener {
            val pairingCode = binding.etPairingCode.text.toString().trim()
            
            if (validatePairingCode(pairingCode)) {
                authViewModel.pairWithParent(pairingCode)
            }
        }
    }

    private fun validatePairingCode(code: String): Boolean {
        if (code.isEmpty()) {
            binding.tilPairingCode.error = "Eşleştirme kodu gerekli"
            return false
        }
        
        if (code.length != 6) {
            binding.tilPairingCode.error = "Eşleştirme kodu 6 haneli olmalı"
            return false
        }
        
        if (!code.all { it.isDigit() }) {
            binding.tilPairingCode.error = "Eşleştirme kodu sadece rakam içermeli"
            return false
        }
        
        return true
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnPair.isEnabled = false
                }
                
                is AuthViewModel.AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPair.isEnabled = true
                    
                    Toast.makeText(context, "Ebeveynle başarıyla eşleştirildi!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_pairingFragment_to_childDashboardFragment)
                }
                
                is AuthViewModel.AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPair.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                
                is AuthViewModel.AuthState.SignedOut -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPair.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
