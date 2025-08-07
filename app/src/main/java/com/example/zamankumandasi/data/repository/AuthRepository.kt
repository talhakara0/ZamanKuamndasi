package com.example.zamankumandasi.data.repository

import com.example.zamankumandasi.data.model.User
import com.example.zamankumandasi.data.model.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
            
            // Firestore'a kullanıcı bilgilerini kaydet
            firestore.collection("users").document(user.id).set(user).await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Kullanıcı bulunamadı")
            
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("Kullanıcı bilgileri bulunamadı")
            
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
        val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
        return userDoc.toObject(User::class.java)
    }
    
    suspend fun pairWithParent(pairingCode: String): Result<User> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Kullanıcı girişi yapılmamış")
            
            // Eşleştirme kodunu ara - daha güvenli sorgu
            val parentQuery = firestore.collection("users")
                .whereEqualTo("pairingCode", pairingCode)
                .whereEqualTo("userType", UserType.PARENT)
                .limit(1) // Sadece 1 sonuç al
                .get().await()
            
            if (parentQuery.isEmpty) {
                throw Exception("Geçersiz eşleştirme kodu")
            }
            
            val parentDoc = parentQuery.documents[0]
            val parent = parentDoc.toObject(User::class.java)
                ?: throw Exception("Ebeveyn bilgileri bulunamadı")
            
            // Çocuk kullanıcısını güncelle
            val updatedChild = User(
                id = currentUser.uid,
                email = currentUser.email ?: "",
                userType = UserType.CHILD,
                parentId = parent.id
            )
            
            // Kendi kullanıcı belgesini güncelle
            firestore.collection("users").document(currentUser.uid).set(updatedChild).await()
            
            Result.success(updatedChild)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChildrenByParent(parentId: String): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("parentId", parentId)
                .whereEqualTo("userType", UserType.CHILD)
                .get().await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun generatePairingCode(): String {
        return (100000..999999).random().toString()
    }
}
