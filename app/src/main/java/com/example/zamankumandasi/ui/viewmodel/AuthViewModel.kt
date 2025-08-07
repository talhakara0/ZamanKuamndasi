package com.example.zamankumandasi.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamankumandasi.data.model.User
import com.example.zamankumandasi.data.model.UserType
import com.example.zamankumandasi.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    private val _children = MutableLiveData<List<User>>()
    val children: LiveData<List<User>> = _children

    init {
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
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null
            _authState.value = AuthState.SignedOut
        }
    }

    fun pairWithParent(pairingCode: String) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = authRepository.pairWithParent(pairingCode)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Eşleştirme başarısız")
                }
            )
        }
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
            
            // Eğer ebeveyn ise çocuklarını yükle
            user?.let { currentUser ->
                if (currentUser.userType == UserType.PARENT) {
                    loadChildren(currentUser.id)
                }
            }
        }
    }
    
    fun loadChildren(parentId: String) {
        viewModelScope.launch {
            val childrenList = authRepository.getChildrenByParent(parentId)
            _children.value = childrenList
        }
    }

    sealed class AuthState {
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
        object SignedOut : AuthState()
    }
}
