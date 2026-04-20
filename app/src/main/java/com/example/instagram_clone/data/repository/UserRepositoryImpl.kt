package com.example.instagram_clone.data.repository

import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.domain.repository.UserRepository
import com.example.instagram_clone.util.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private fun docToUser(doc: com.google.firebase.firestore.DocumentSnapshot): User? {
        return try {
            User(
                id = doc.id,
                email = doc.getString("email") ?: "",
                username = doc.getString("username") ?: "",
                displayName = doc.getString("displayName") ?: "",
                bio = doc.getString("bio") ?: "",
                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                isPrivate = doc.getBoolean("isPrivate") ?: false,
                createdAt = doc.getTimestamp("createdAt")
            )
        } catch (e: Exception) { null }
    }

    override fun getUserById(userId: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val doc = db.collection("Users").document(userId).get().await()
            if (doc.exists()) {
                docToUser(doc)?.let { emit(Resource.Success(it)) }
                    ?: emit(Resource.Error("Kullanıcı verileri okunamadı"))
            } else {
                emit(Resource.Error("Kullanıcı bulunamadı"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.localizedMessage ?: "Hata oluştu"))
        }
    }

    override fun getUserByEmail(email: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = db.collection("Users")
                .whereEqualTo("email", email).limit(1).get().await()
            if (!snapshot.isEmpty) {
                docToUser(snapshot.documents[0])?.let { emit(Resource.Success(it)) }
                    ?: emit(Resource.Error("Kullanıcı verileri okunamadı"))
            } else {
                emit(Resource.Error("Kullanıcı bulunamadı"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.localizedMessage ?: "Hata oluştu"))
        }
    }

    override fun createUserProfile(user: User): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val userMap = hashMapOf<String, Any>(
                "email" to user.email,
                "username" to user.username,
                "displayName" to user.displayName,
                "bio" to user.bio,
                "profileImageUrl" to user.profileImageUrl,
                "isPrivate" to user.isPrivate,
                "createdAt" to (user.createdAt ?: Timestamp.now())
            )
            db.collection("Users").document(user.id).set(userMap).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.localizedMessage ?: "Profil oluşturulamadı"))
        }
    }

    override fun updateUserProfile(userId: String, fields: Map<String, Any>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            db.collection("Users").document(userId).update(fields).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.localizedMessage ?: "Profil güncellenemedi"))
        }
    }

    override fun searchUsers(query: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        try {
            val lowerQuery = query.lowercase().trim()
            val currentUid = auth.currentUser?.uid ?: ""


            val snapshot = db.collection("Users")
                .whereGreaterThanOrEqualTo("username", lowerQuery)
                .whereLessThan("username", lowerQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                docToUser(doc)
            }.filter { it.id != currentUid }

            emit(Resource.Success(users))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Arama sınırları aşıldı veya veritabanı indeks hatası oluştu."))
        }
    }

    override fun getOrCreateCurrentUserProfile(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val currentUser = auth.currentUser ?: throw Exception("Oturum açılmamış")
            val uid = currentUser.uid

            val doc = db.collection("Users").document(uid).get().await()
            if (doc.exists()) {
                docToUser(doc)?.let { emit(Resource.Success(it)) }
                    ?: emit(Resource.Error("Profil okunamadı"))
            } else {

                val email = currentUser.email ?: ""
                val username = email.substringBefore("@").lowercase()
                val newUser = User(
                    id = uid,
                    email = email,
                    username = username,
                    displayName = username,
                    createdAt = Timestamp.now()
                )
                val userMap = hashMapOf<String, Any>(
                    "email" to email,
                    "username" to username,
                    "displayName" to username,
                    "bio" to "",
                    "profileImageUrl" to "",
                    "isPrivate" to false,
                    "createdAt" to Timestamp.now()
                )
                db.collection("Users").document(uid).set(userMap).await()
                emit(Resource.Success(newUser))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.localizedMessage ?: "Hata oluştu"))
        }
    }

    override fun uploadProfilePhoto(userId: String, imageBytes: ByteArray): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
            val ref = storage.reference.child("posts").child(userId).child("profile_${java.util.UUID.randomUUID()}.jpg")
            ref.putBytes(imageBytes).await()
            val uri = ref.downloadUrl.await()
            emit(Resource.Success(uri.toString()))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Fotoğraf yüklenemedi"))
        }
    }
}
