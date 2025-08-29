package com.talhadev.zamankumandasi.utils

import android.app.AlertDialog
import android.util.Log
import androidx.fragment.app.Fragment
import com.talhadev.zamankumandasi.ui.viewmodel.AuthViewModel

object SimpleLogout {
    fun confirmAndSignOut(fragment: Fragment, authViewModel: AuthViewModel) {
        try {
            AlertDialog.Builder(fragment.requireContext())
                .setTitle("Çıkış Yap")
                .setMessage("Çıkış yapmak istediğinize emin misiniz?")
                .setPositiveButton("Evet") { dialog, _ ->
                    dialog.dismiss()
                    Log.d("talha", "User confirmed sign out")
                    authViewModel.signOut()
                }
                .setNegativeButton("Hayır") { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e("talha", "confirmAndSignOut error: ${e.message}")
            // Fallback: doğrudan signOut
            authViewModel.signOut()
        }
    }
}
