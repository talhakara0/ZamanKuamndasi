package com.example.zamankumandasi.data.repository

import com.example.zamankumandasi.data.model.User
import com.example.zamankumandasi.data.model.UserType
import com.example.zamankumandasi.data.manager.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val sessionManager: SessionManager
) {
    
    suspend fun signUp(email: String, password: String, userType: UserType): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = User(
                id = result.user?.uid ?: "",
                email = email,
                userType = userType,
                parentId = if (userType == UserType.PARENT) null else null, // Ebeveyn için parentId null olarak ekleniyor
                pairingCode = if (userType == UserType.PARENT) generatePairingCode() else null
            )
            
            // RTDB'ye kullanıcıyı kaydet
            database.reference.child("users").child(user.id).setValue(user).await()
            
            // Session kaydet - tüm bilgilerle
            sessionManager.setLoginSession(user.id, user.email, user.userType.name, user.parentId, user.pairingCode)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // Önce local session kontrol et - eğer aynı email ile session varsa direkt giriş yap
            val localUser = createUserFromSession()
            if (localUser != null && localUser.email == email) {
                return Result.success(localUser)
            }
            
            // Firebase ile giriş yapmaya çalış
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: throw Exception("Kullanıcı bulunamadı")
                
                val userSnapshot = database.reference.child("users").child(userId).get().await()
                val user = userSnapshot.getValue(User::class.java) ?: throw Exception("Kullanıcı bilgileri bulunamadı")
                
                // Session kaydet - tüm bilgilerle
                sessionManager.setLoginSession(user.id, user.email, user.userType.name, user.parentId, user.pairingCode)
                
                Result.success(user)
            } catch (e: Exception) {
                // İnternet yoksa veya Firebase hatası varsa, local session kontrol et
                val cachedUser = createUserFromSession()
                if (cachedUser != null && cachedUser.email == email) {
                    Result.success(cachedUser)
                } else {
                    Result.failure(Exception("İnternet bağlantısı yok ve local session bulunamadı: ${e.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            // Firebase Auth'dan çıkış yap
            auth.signOut()
            
            // Session temizle
            sessionManager.clearSession()
            
            // Başka temizleme işlemleri burada yapılabilir
            // Örneğin: local cache temizleme, shared preferences temizleme vs.
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(): User? {
        return try {
            // Session kontrolü yap - sadece login durumunu kontrol et
            if (!sessionManager.isSessionValid()) {
                return null
            }
            
            // Firebase'den kullanıcı bilgilerini almaya çalış
            val currentUser = auth.currentUser
            if (currentUser != null) {
                try {
                    val userSnapshot = database.reference.child("users").child(currentUser.uid).get().await()
                    val user = userSnapshot.getValue(User::class.java)
                    
                    // Firebase'den veri gelirse session'ı güncelle - tüm bilgilerle
                    user?.let {
                        sessionManager.setLoginSession(it.id, it.email, it.userType.name, it.parentId, it.pairingCode)
                    }
                    return user
                } catch (e: Exception) {
                    // İnternet bağlantısı yoksa veya Firebase hatası varsa local session'dan döndür
                    return createUserFromSession()
                }
            } else {
                // Firebase Auth'da kullanıcı yoksa ama local session varsa local'dan döndür
                return createUserFromSession()
            }
        } catch (e: Exception) {
            // Herhangi bir hata durumunda local session'dan döndür
            return createUserFromSession()
        }
    }
    
    private fun createUserFromSession(): User? {
        val userId = sessionManager.getUserId()
        val email = sessionManager.getUserEmail()
        val userTypeString = sessionManager.getUserType()
        val parentId = sessionManager.getParentId()
        val pairingCode = sessionManager.getPairingCode()
        
        return if (userId != null && email != null && userTypeString != null) {
            try {
                val userType = UserType.valueOf(userTypeString)
                User(
                    id = userId,
                    email = email,
                    userType = userType,
                    parentId = parentId,
                    pairingCode = pairingCode
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    suspend fun pairWithParent(pairingCode: String): Result<User> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Kullanıcı girişi yapılmamış")
            
            // RTDB'de pairingCode ile ebeveyn arama
            val parentQuery = database.reference.child("users").orderByChild("pairingCode").equalTo(pairingCode).get().await()
            val parentSnapshot = parentQuery.children.firstOrNull { it.child("userType").getValue(String::class.java) == UserType.PARENT.name }
            if (parentSnapshot == null) {
                throw Exception("Geçersiz eşleştirme kodu")
            }
            
            val parent = parentSnapshot.getValue(User::class.java) ?: throw Exception("Ebeveyn bilgileri bulunamadı")
            
            // Çocuk kullanıcısını güncelle
            val updatedChild = User(
                id = currentUser.uid,
                email = currentUser.email ?: "",
                userType = UserType.CHILD,
                parentId = parent.id
            )
            
            // Kendi kullanıcı belgesini güncelle
            database.reference.child("users").child(currentUser.uid).setValue(updatedChild).await()
            
            Result.success(updatedChild)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChildrenByParent(parentId: String): List<User> {
        return try {
            val snapshot = database.reference.child("users").orderByChild("parentId").equalTo(parentId).get().await()
            snapshot.children.mapNotNull { it.getValue(User::class.java) }.filter { it.userType == UserType.CHILD }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun generatePairingCode(): String {
        return (100000..999999).random().toString()
    }
}
