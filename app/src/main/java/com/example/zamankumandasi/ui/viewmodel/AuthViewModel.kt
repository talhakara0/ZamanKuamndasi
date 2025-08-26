package com.example.zamankumandasi.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamankumandasi.billing.BillingManager
import com.example.zamankumandasi.data.model.User
import com.example.zamankumandasi.data.model.UserType
import com.example.zamankumandasi.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    private val _children = MutableLiveData<List<User>>()
    val children: LiveData<List<User>> = _children
    
    private val _parentInfo = MutableLiveData<User?>()
    val parentInfo: LiveData<User?> = _parentInfo

    // Eşleştirme sonuçları için ayrı state (authState'den bağımsız)
    private val _pairingState = MutableLiveData<PairingState?>()
    val pairingState: LiveData<PairingState?> = _pairingState

    init {
        // Başlangıçta pairing state null
        _pairingState.value = null
        checkCurrentUser()
    }

    fun signUp(email: String, password: String, userType: UserType) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signUp(email, password, userType)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Kayıt başarısız")
                }
            )
        }
    }

    fun signIn(email: String, password: String) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Giriş başarısız")
                }
            )
        }
    }

    fun signOut() {
        _authState.value = AuthState.Loading
        
        android.util.Log.d("talha", "signOut: başlatıldı")

        viewModelScope.launch {
            try {
                // 1. Repository'den çıkış yap
                val result = authRepository.signOut()

                result.fold(
                    onSuccess = {
                        android.util.Log.d("talha", "signOut: repository başarılı döndü - clearUserData çağrılıyor")
                        // 2. Tüm kullanıcı verilerini temizle
                        clearUserData()
                        
                        // 3. State'i güncelle
                        _authState.value = AuthState.SignedOut
                    },
                    onFailure = { exception ->
                        android.util.Log.e("talha", "signOut: repository hata: ${exception.message}")
                        _authState.value = AuthState.Error("Çıkış işlemi sırasında hata: ${exception.message}")
                    }
                )
                
            } catch (e: Exception) {
                android.util.Log.e("talha", "signOut exception: ${e.message}")
                _authState.value = AuthState.Error("Çıkış işlemi sırasında hata: ${e.message}")
            }
        }
    }
    
    private fun clearUserData() {
        _currentUser.value = null
        _children.value = emptyList()
        _parentInfo.value = null
        // Diğer kullanıcı verilerini de temizle
    }

    fun pairWithParent(pairingCode: String) {
        _pairingState.value = PairingState.Loading
        // İstenirse eşleştirme boyunca genel authState'i etkilemeyebiliriz; mevcut ekranlar pairingState'i dinleyecek
        viewModelScope.launch {
            val result = authRepository.pairWithParent(pairingCode)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _pairingState.value = PairingState.Success(user)
                    // Opsiyonel: genel authState'i de güncelle
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _pairingState.value = PairingState.Error(exception.message ?: "Eşleştirme başarısız")
                }
            )
        }
    }

    fun clearPairingState() {
        _pairingState.value = null
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                _currentUser.value = user
                
                // Kullanıcı varsa AuthState'i Success yap (ama sadece ilk yüklemede)
                if (user != null && _authState.value !is AuthState.Success) {
                    _authState.value = AuthState.Success(user)
                }
                
                // Eğer ebeveyn ise çocuklarını yükle
                user?.let { currentUser ->
                    if (currentUser.userType == UserType.PARENT) {
                        loadChildren(currentUser.id)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("talha", "checkCurrentUser error: ${e.message}")
            }
        }
    }
    
    fun loadChildren(parentId: String) {
        viewModelScope.launch {
            val childrenList = authRepository.getChildrenByParent(parentId)
            _children.value = childrenList
        }
    }
    
    fun loadParentInfo(childId: String) {
        viewModelScope.launch {
            val parent = authRepository.getParentByChildId(childId)
            _parentInfo.value = parent
        }
    }

    sealed class AuthState {
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
        object SignedOut : AuthState()
    }

    sealed class PairingState {
        object Loading : PairingState()
        data class Success(val user: User) : PairingState()
        data class Error(val message: String) : PairingState()
    }

    fun setPremiumForCurrentUser(enabled: Boolean) {
        viewModelScope.launch {
            val result = authRepository.setPremiumForCurrentUser(enabled)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { ex ->
                    _authState.value = AuthState.Error(ex.message ?: "Premium güncellenemedi")
                }
            )
        }
    }
    
    fun updatePremiumFromBilling() {
        viewModelScope.launch {
            val isPremiumPurchased = billingManager.isPremiumPurchased()
            val currentUser = _currentUser.value
            
            // Eğer satın alma durumu ile kullanıcı durumu farklıysa güncelle
            if (currentUser != null && currentUser.isPremium != isPremiumPurchased) {
                setPremiumForCurrentUser(isPremiumPurchased)
            }
        }
    }
}
