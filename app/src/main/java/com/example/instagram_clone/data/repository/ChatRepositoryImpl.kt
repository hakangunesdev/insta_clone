package com.example.instagram_clone.data.repository

import com.example.instagram_clone.domain.model.Chat
import com.example.instagram_clone.domain.model.Message
import com.example.instagram_clone.domain.repository.ChatRepository
import com.example.instagram_clone.util.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ChatRepository {

    /**
     * Retrieves or creates a chat session with the specified user.
     *
     * @param otherUserId The target user's ID.
     * @return A Resource containing the specific Chat ID string.
     */
    override suspend fun getOrCreateChatId(otherUserId: String): Resource<String> {
        val currentUser = auth.currentUser ?: return Resource.Error("Oturum bulunamadı")
        val currentUid = currentUser.uid
        val chatId = generateChatId(currentUid, otherUserId)
        
        try {
            val doc = db.collection("Chats").document(chatId).get().await()
            if (!doc.exists()) {
                val chatMap = hashMapOf(
                    "participantIds" to listOf(currentUid, otherUserId),
                    "timestamp" to Timestamp.now()
                )
                db.collection("Chats").document(chatId).set(chatMap).await()
            }
            return Resource.Success(chatId)
        } catch (e: Exception) {
            return Resource.Error(e.localizedMessage ?: "Sohbet oluşturulamadı")
        }
    }

    /**
     * Subscribes to the list of conversations where the current user is a participant.
     *
     * @return A Flow emitting the latest list of [Chat] objects.
     */
    override fun getConversations(): Flow<Resource<List<Chat>>> = callbackFlow {
        trySend(Resource.Loading())
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(Resource.Error("Oturum bulunamadı"))
            close()
            return@callbackFlow
        }

        val listener = db.collection("Chats")
            .whereArrayContains("participantIds", currentUid)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Sohbetler alınamadı"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        try {
                            val participants = doc.get("participantIds") as? List<String> ?: emptyList()
                            if (currentUid !in participants) return@mapNotNull null
                            
                            val otherUserId = participants.firstOrNull { it != currentUid } ?: ""
                            
                            Chat(
                                id = doc.id,
                                participantIds = participants,
                                otherUserId = otherUserId,
                                lastMessage = doc.getString("lastMessage") ?: "",
                                lastSenderId = doc.getString("lastSenderId") ?: "",
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        } catch (e: Exception) { null }
                    }.sortedByDescending { it.timestamp }
                    trySend(Resource.Success(chats))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Subscribes to messages within a specific chat.
     *
     * @param chatId The unique ID of the chat document.
     * @return A Flow emitting the list of [Message] objects in the chat.
     */
    override fun getMessages(chatId: String): Flow<Resource<List<Message>>> = callbackFlow {
        trySend(Resource.Loading())
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(Resource.Error("Oturum bulunamadı"))
            close()
            return@callbackFlow
        }

        val listener = db.collection("Chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Mesajlar alınamadı"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            Message(
                                id = doc.id,
                                senderId = doc.getString("senderId") ?: "",
                                text = doc.getString("text") ?: "",
                                timestamp = doc.getTimestamp("timestamp"),
                                postId = doc.getString("postId") ?: "",
                                postImageUrl = doc.getString("postImageUrl") ?: ""
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(Resource.Success(messages))
                }
            }
            
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Sends a text message or shared post to the specified chat session.
     * Updates the chat's outer metadata containing the most recent message info.
     *
     * @param chatId The ID of the chat.
     * @param text The message text content.
     * @param postId Optional ID of a shared post.
     * @param postImageUrl Optional image URL of the shared post.
     * @return A Resource representing the operation status.
     */
    override suspend fun sendMessage(chatId: String, text: String, postId: String?, postImageUrl: String?): Resource<Unit> {
        val currentUid = auth.currentUser?.uid ?: return Resource.Error("Oturum bulunamadı")
        
        return try {
            val batch = db.batch()
            val messageRef = db.collection("Chats").document(chatId)
                .collection("messages").document()
            
            val messageMap = hashMapOf<String, Any>(
                "senderId" to currentUid,
                "text" to text,
                "timestamp" to Timestamp.now()
            ).apply {
                if (!postId.isNullOrEmpty()) put("postId", postId)
                if (!postImageUrl.isNullOrEmpty()) put("postImageUrl", postImageUrl)
            }
            batch.set(messageRef, messageMap)
            
            val chatRef = db.collection("Chats").document(chatId)
            val chatUpdateMap = hashMapOf<String, Any>(
                "lastMessage" to text,
                "lastSenderId" to currentUid,
                "timestamp" to Timestamp.now()
            )
            batch.update(chatRef, chatUpdateMap)
            
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Mesaj gönderilemedi")
        }
    }

    private fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }
}
