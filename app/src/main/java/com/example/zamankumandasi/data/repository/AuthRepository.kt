package com.example.zamankumandasi.data.repository

import com.example.zamankumandasi.data.model.User
import com.example.zamankumandasi.data.model.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    
    suspend fun signUp(email: String, password: String, userType: UserType): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = User(
                id = result.user?.uid ?: "",
                email = email,
                userType = userType,
                pairingCode = if (userType == UserType.PARENT) generatePairingCode() else null
            )
            
            // RTDB'ye kullanıcıyı kaydet
            database.reference.child("users").child(user.id).setValue(user).await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Kullanıcı bulunamadı")
            
            val userSnapshot = database.reference.child("users").child(userId).get().await()
            val user = userSnapshot.getValue(User::class.java) ?: throw Exception("Kullanıcı bilgileri bulunamadı")
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut() {
        auth.signOut()
    }
    
    suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser ?: return null
        val userSnapshot = database.reference.child("users").child(currentUser.uid).get().await()
        return userSnapshot.getValue(User::class.java)
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
