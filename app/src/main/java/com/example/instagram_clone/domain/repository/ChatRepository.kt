package com.example.instagram_clone.domain.repository

import com.example.instagram_clone.domain.model.Chat
import com.example.instagram_clone.domain.model.Message
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    
    /**
     * İki kullanıcı arasındaki chatId'yi (Room Id) döndürür veya oluşturur.
     */
    suspend fun getOrCreateChatId(otherUserId: String): Resource<String>
    
    /**
     * O anki kullanıcının aktif mesajlaşmalarını (ChatList) getirir.
     */
    fun getConversations(): Flow<Resource<List<Chat>>>
    
    /**
     * Belirli bir chatId için gönderilen tüm mesajları getirir.
     */
    fun getMessages(chatId: String): Flow<Resource<List<Message>>>
    
    /**
     * Bir chat id'sine mesaj gönderir. Eş zamanlı olarak Chat belgesindeki lastMessage alanını günceller.
     */
    suspend fun sendMessage(chatId: String, text: String, postId: String? = null, postImageUrl: String? = null): Resource<Unit>
}
