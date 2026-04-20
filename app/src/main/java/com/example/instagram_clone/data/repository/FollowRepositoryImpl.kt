package com.example.instagram_clone.data.repository

import com.example.instagram_clone.domain.model.FollowStatus
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.domain.repository.FollowRepository
import com.example.instagram_clone.util.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FollowRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FollowRepository {

    private val currentUserId: String get() = auth.currentUser?.uid ?: ""

    override fun followUser(targetUserId: String, targetUser: User): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val currentUser = auth.currentUser ?: run {
            trySend(Resource.Error("Oturum açılmamış"))
            close(); return@callbackFlow
        }

        val batch = db.batch()


        val followingRef = db.collection("Users").document(currentUserId)
            .collection("following").document(targetUserId)
        batch.set(followingRef, hashMapOf(
            "email" to targetUser.email,
            "username" to targetUser.username,
            "date" to Timestamp.now()
        ))


        val followerRef = db.collection("Users").document(targetUserId)
            .collection("followers").document(currentUserId)
        batch.set(followerRef, hashMapOf(
            "email" to (currentUser.email ?: ""),
            "username" to (currentUser.email?.substringBefore("@") ?: ""),
            "date" to Timestamp.now()
        ))

        batch.commit()
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.localizedMessage ?: "Takip edilemedi"))
                close()
            }
        awaitClose()
    }

    override fun unfollowUser(targetUserId: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val batch = db.batch()

        val followingRef = db.collection("Users").document(currentUserId)
            .collection("following").document(targetUserId)
        batch.delete(followingRef)

        val followerRef = db.collection("Users").document(targetUserId)
            .collection("followers").document(currentUserId)
        batch.delete(followerRef)

        batch.commit()
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.localizedMessage ?: "Takipten çıkılamadı"))
                close()
            }
        awaitClose()
    }

    override fun sendFollowRequest(targetUserId: String, targetUser: User): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val currentUser = auth.currentUser ?: run {
            trySend(Resource.Error("Oturum açılmamış"))
            close(); return@callbackFlow
        }

        val requestRef = db.collection("Users").document(targetUserId)
            .collection("followRequests").document(currentUserId)
        requestRef.set(hashMapOf(
            "email" to (currentUser.email ?: ""),
            "username" to (currentUser.email?.substringBefore("@") ?: ""),
            "date" to Timestamp.now()
        ))
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.localizedMessage ?: "İstek gönderilemedi"))
                close()
            }
        awaitClose()
    }

    override fun acceptFollowRequest(requesterId: String, requesterUser: User): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val currentUser = auth.currentUser ?: run {
            trySend(Resource.Error("Oturum açılmamış"))
            close(); return@callbackFlow
        }

        val batch = db.batch()


        val requestRef = db.collection("Users").document(currentUserId)
            .collection("followRequests").document(requesterId)
        batch.delete(requestRef)


        val followerRef = db.collection("Users").document(currentUserId)
            .collection("followers").document(requesterId)
        batch.set(followerRef, hashMapOf(
            "email" to requesterUser.email,
            "username" to requesterUser.username,
            "date" to Timestamp.now()
        ))


        val followingRef = db.collection("Users").document(requesterId)
            .collection("following").document(currentUserId)
        batch.set(followingRef, hashMapOf(
            "email" to (currentUser.email ?: ""),
            "username" to (currentUser.email?.substringBefore("@") ?: ""),
            "date" to Timestamp.now()
        ))

        batch.commit()
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.localizedMessage ?: "İstek kabul edilemedi"))
                close()
            }
        awaitClose()
    }

    override fun rejectFollowRequest(requesterId: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        db.collection("Users").document(currentUserId)
            .collection("followRequests").document(requesterId).delete()
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.localizedMessage ?: "İstek reddedilemedi"))
                close()
            }
        awaitClose()
    }

    override fun cancelFollowRequest(targetUserId: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        db.collection("Users").document(targetUserId)
            .collection("followRequests").document(currentUserId).delete()
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.localizedMessage ?: "İstek iptal edilemedi"))
                close()
            }
        awaitClose()
    }

    override fun getFollowers(userId: String): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = db.collection("Users").document(userId)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata"))
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    User(
                        id = doc.id,
                        email = doc.getString("email") ?: "",
                        username = doc.getString("username") ?: ""
                    )
                } ?: emptyList()
                trySend(Resource.Success(users))
            }
        awaitClose { listener.remove() }
    }

    override fun getFollowing(userId: String): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = db.collection("Users").document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata"))
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    User(
                        id = doc.id,
                        email = doc.getString("email") ?: "",
                        username = doc.getString("username") ?: ""
                    )
                } ?: emptyList()
                trySend(Resource.Success(users))
            }
        awaitClose { listener.remove() }
    }

    override fun getFollowRequests(): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = db.collection("Users").document(currentUserId)
            .collection("followRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata"))
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    User(
                        id = doc.id,
                        email = doc.getString("email") ?: "",
                        username = doc.getString("username") ?: ""
                    )
                } ?: emptyList()
                trySend(Resource.Success(users))
            }
        awaitClose { listener.remove() }
    }

    override fun getFollowStatus(targetUserId: String): Flow<Resource<FollowStatus>> = callbackFlow {
        trySend(Resource.Loading())


        db.collection("Users").document(currentUserId)
            .collection("following").document(targetUserId).get()
            .addOnSuccessListener { followDoc ->
                if (followDoc.exists()) {
                    trySend(Resource.Success(FollowStatus.FOLLOWING))
                    close()
                } else {

                    db.collection("Users").document(targetUserId)
                        .collection("followRequests").document(currentUserId).get()
                        .addOnSuccessListener { reqDoc ->
                            if (reqDoc.exists()) {
                                trySend(Resource.Success(FollowStatus.REQUESTED))
                            } else {
                                trySend(Resource.Success(FollowStatus.NONE))
                            }
                            close()
                        }
                        .addOnFailureListener {
                            trySend(Resource.Success(FollowStatus.NONE))
                            close()
                        }
                }
            }
            .addOnFailureListener {
                trySend(Resource.Success(FollowStatus.NONE))
                close()
            }
        awaitClose()
    }

    override fun getFollowersCount(userId: String): Flow<Resource<Int>> = callbackFlow {
        val listener = db.collection("Users").document(userId)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata"))
                    return@addSnapshotListener
                }
                trySend(Resource.Success(snapshot?.size() ?: 0))
            }
        awaitClose { listener.remove() }
    }

    override fun getFollowingCount(userId: String): Flow<Resource<Int>> = callbackFlow {
        val listener = db.collection("Users").document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata"))
                    return@addSnapshotListener
                }
                trySend(Resource.Success(snapshot?.size() ?: 0))
            }
        awaitClose { listener.remove() }
    }
}
