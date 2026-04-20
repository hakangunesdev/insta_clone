package com.example.instagram_clone.data.repository

import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.domain.repository.AuthRepository
import com.example.instagram_clone.util.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    /**
     * Authenticates a user with Firebase Auth and ensures their profile exists in Firestore.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return A Flow emitting the authentication Resource state.
     */
    override fun signIn(email: String, password: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Kullanıcı bulunamadı")

            try {
                val doc = db.collection("Users").document(user.uid).get().await()
                if (!doc.exists()) {
                    val username = email.substringBefore("@").lowercase()
                    val userMap = hashMapOf<String, Any>(
                        "email" to email,
                        "username" to username,
                        "displayName" to username,
                        "bio" to "",
                        "profileImageUrl" to "",
                        "isPrivate" to false,
                        "createdAt" to Timestamp.now()
                    )
                    db.collection("Users").document(user.uid).set(userMap).await()
                }
            } catch (_: Exception) {
            }

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Giriş başarısız"))
        }
    }

    /**
     * Registers a new user with Firebase Auth and creates their initial profile in Firestore.
     *
     * @param email The new user's email address.
     * @param password The requested password.
     * @param username The chosen username.
     * @return A Flow emitting the registration Resource state.
     */
    override fun signUp(email: String, password: String, username: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Kayıt başarısız")

            val userMap = hashMapOf<String, Any>(
                "email" to email,
                "username" to username.lowercase().trim(),
                "displayName" to username.trim(),
                "bio" to "",
                "profileImageUrl" to "",
                "isPrivate" to false,
                "createdAt" to Timestamp.now()
            )
            db.collection("Users").document(user.uid).set(userMap).await()

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Kayıt başarısız"))
        }
    }

    /**
     * Signs out the current authenticated user.
     */
    override fun signOut() {
        auth.signOut()
    }

    /**
     * Retrieves the current authenticated user profile mapped from Firebase properties.
     *
     * @return The [User] entity, or null if no user is authenticated.
     */
    override fun getCurrentUser(): User? {
        val fbUser = auth.currentUser ?: return null
        return User(
            id = fbUser.uid,
            username = fbUser.email?.substringBefore("@") ?: "user",
            displayName = fbUser.displayName ?: "User",
            email = fbUser.email ?: "",
            profileImageUrl = fbUser.photoUrl?.toString() ?: "",
            bio = ""
        )
    }

    /**
     * Safely retrieves the current user's unique identifier.
     *
     * @return The user ID, or an empty string if not authenticated.
     */
    override fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""
}
