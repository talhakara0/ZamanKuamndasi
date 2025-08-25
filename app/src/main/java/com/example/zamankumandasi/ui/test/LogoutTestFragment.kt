package com.example.zamankumandasi.ui.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.zamankumandasi.R
import com.example.zamankumandasi.data.manager.LogoutManager
import com.example.zamankumandasi.ui.viewmodel.AuthViewModel
import com.example.zamankumandasi.utils.performLogoutWithManager
import com.example.zamankumandasi.utils.performQuickLogout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LogoutTestFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    
    @Inject
    lateinit var logoutManager: LogoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logout_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnNormalLogout = view.findViewById<Button>(R.id.btn_normal_logout)
        val btnQuickLogout = view.findViewById<Button>(R.id.btn_quick_logout)
        val tvStatus = view.findViewById<TextView>(R.id.tv_status)

        // Normal logout (onay dialog'u ile)
        btnNormalLogout.setOnClickListener {
            performLogoutWithManager(authViewModel, logoutManager)
        }

        // Hızlı logout (onay dialog'u olmadan)
        btnQuickLogout.setOnClickListener {
            performQuickLogout(authViewModel, logoutManager)
        }

        // Auth durumunu gözle
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            tvStatus.text = when (state) {
                is AuthViewModel.AuthState.Loading -> "Yükleniyor..."
                is AuthViewModel.AuthState.Success -> "Giriş yapılmış: ${state.user.email}"
                is AuthViewModel.AuthState.Error -> "Hata: ${state.message}"
                is AuthViewModel.AuthState.SignedOut -> "Çıkış yapılmış"
            }
        }

        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                tvStatus.text = "Kullanıcı: Yok"
            }
        }
    }
}
