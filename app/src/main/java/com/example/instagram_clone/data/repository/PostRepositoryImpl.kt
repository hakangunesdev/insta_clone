package com.example.instagram_clone.data.repository


import com.example.instagram_clone.domain.model.Comment
import com.example.instagram_clone.domain.model.Post
import com.example.instagram_clone.domain.repository.PostRepository
import com.example.instagram_clone.util.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

import java.util.UUID

class PostRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : PostRepository {

    @Suppress("UNCHECKED_CAST")
    private fun documentToPost(doc: com.google.firebase.firestore.DocumentSnapshot): Post? {
        return try {
            Post(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                email = doc.getString("userEmail") ?: "",
                username = doc.getString("username") ?: "",
                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                comment = doc.getString("comment") ?: "",
                downloadUrl = doc.getString("downloadUrl") ?: "",
                date = doc.getTimestamp("date"),
                likes = (doc.get("likes") as? List<String>) ?: emptyList(),
                commentCount = (doc.getLong("commentCount") ?: 0).toInt()
            )
        } catch (e: Exception) { null }
    }

    override fun getPosts(): Flow<Resource<List<Post>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = db.collection("Posts")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    trySend(Resource.Error(exception.localizedMessage ?: "Veriler alınamadı"))
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val posts = snapshot.documents.mapNotNull { documentToPost(it) }
                    trySend(Resource.Success(posts))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override fun getPostsByUserId(userId: String): Flow<Resource<List<Post>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = db.collection("Posts")
            .whereEqualTo("userId", userId)

            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    trySend(Resource.Error(exception.localizedMessage ?: "Veriler alınamadı"))
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val posts = snapshot.documents.mapNotNull { documentToPost(it) }
                        .sortedByDescending { it.date }
                    trySend(Resource.Success(posts))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Uploads a new post along with an image to Firebase Storage and Firestore.
     * Uses suspend functions internally to avoid hanging flows and nested callbacks.
     *
     * @param bitmap The image bitmap to upload.
     * @param comment The caption for the post.
     * @return A Flow emitting the upload progress/result.
     */
    override fun uploadPost(imageBytes: ByteArray, comment: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val currentUser = auth.currentUser
                ?: throw Exception("Kullanıcı oturumu bulunamadı")

            val uid = currentUser.uid

            var username = currentUser.email?.substringBefore("@") ?: ""
            var profileImageUrl = ""
            try {
                val userDoc = db.collection("Users").document(uid).get().await()
                if (userDoc.exists()) {
                    username = userDoc.getString("username") ?: username
                    profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                }
            } catch (_: Exception) {
            }

            val imageName = "${UUID.randomUUID()}.jpg"
            val imageRef = storage.reference.child("posts").child(uid).child(imageName)
            imageRef.putBytes(imageBytes).await()

            val downloadUrl = imageRef.downloadUrl.await().toString()

            val postMap = hashMapOf<String, Any>(
                "downloadUrl" to downloadUrl,
                "userId" to uid,
                "userEmail" to (currentUser.email ?: ""),
                "username" to username,
                "profileImageUrl" to profileImageUrl,
                "comment" to comment,
                "date" to Timestamp.now(),
                "likes" to emptyList<String>(),
                "commentCount" to 0
            )
            db.collection("Posts").add(postMap).await()

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Gönderi paylaşılamadı"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Toggles the like status of a specific post for the current user.
     *
     * @param postId The ID of the post to like or unlike.
     * @return A Flow emitting the resulting Resource state.
     */
    override fun toggleLike(postId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val uid = auth.currentUser?.uid ?: throw Exception("Oturum açılmamış")
            val postRef = db.collection("Posts").document(postId)
            val doc = postRef.get().await()

            @Suppress("UNCHECKED_CAST")
            val likes = (doc.get("likes") as? List<String>) ?: emptyList()
            if (likes.contains(uid)) {
                postRef.update("likes", FieldValue.arrayRemove(uid)).await()
            } else {
                postRef.update("likes", FieldValue.arrayUnion(uid)).await()
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Beğeni hatası"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getComments(postId: String): Flow<Resource<List<Comment>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = db.collection("Posts").document(postId)
            .collection("comments")
            .orderBy("date", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Yorumlar alınamadı"))
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Comment(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            username = doc.getString("username") ?: "",
                            profileImageUrl = doc.getString("profileImageUrl") ?: "",
                            text = doc.getString("text") ?: "",
                            date = doc.getTimestamp("date")
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(Resource.Success(comments))
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override fun addComment(postId: String, text: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val currentUser = auth.currentUser ?: throw Exception("Oturum açılmamış")
            val uid = currentUser.uid


            var username = currentUser.email?.substringBefore("@") ?: ""
            var profileImageUrl = ""
            try {
                val userDoc = db.collection("Users").document(uid).get().await()
                if (userDoc.exists()) {
                    username = userDoc.getString("username") ?: username
                    profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                }
            } catch (_: Exception) {}

            val commentMap = hashMapOf<String, Any>(
                "userId" to uid,
                "username" to username,
                "profileImageUrl" to profileImageUrl,
                "text" to text,
                "date" to Timestamp.now()
            )

            val batch = db.batch()
            val commentRef = db.collection("Posts").document(postId)
                .collection("comments").document()
            batch.set(commentRef, commentMap)
            val postRef = db.collection("Posts").document(postId)
            batch.update(postRef, "commentCount", FieldValue.increment(1))
            batch.commit().await()

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Yorum eklenemedi"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getPostById(postId: String): Flow<Resource<Post>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = db.collection("Posts").document(postId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata"))
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    documentToPost(doc)?.let { trySend(Resource.Success(it)) }
                } else {
                    trySend(Resource.Error("Gönderi bulunamadı"))
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateProfileImageInPosts(userId: String, imageUrl: String) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val querySnapshot = db.collection("Posts")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val batch = db.batch()
                    for (doc in querySnapshot.documents) {
                        batch.update(doc.reference, "profileImageUrl", imageUrl)
                    }
                    batch.commit().await()
                }
            } catch (_: Exception) {

            }
        }
    }
}
