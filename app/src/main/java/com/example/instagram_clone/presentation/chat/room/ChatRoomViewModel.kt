package com.example.instagram_clone.presentation.chat.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.Message
import com.example.instagram_clone.data.repository.AuthRepositoryImpl
import com.example.instagram_clone.data.repository.ChatRepositoryImpl
import com.example.instagram_clone.domain.repository.AuthRepository
import com.example.instagram_clone.domain.repository.ChatRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatRoomViewModel(
    private val chatRepository: ChatRepository = ChatRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _messages = MutableLiveData<Resource<List<Message>>>()
    val messages: LiveData<Resource<List<Message>>> = _messages

    private val _sendState = MutableLiveData<Resource<Unit>>()
    val sendState: LiveData<Resource<Unit>> = _sendState

    private var currentChatId: String? = null

    fun getCurrentUserId(): String = authRepository.getCurrentUserId()

    /**
     * Initializes the chat session. Generates or fetches the chatId if it does not exist.
     * Starts listening to incoming messages for the determined chat ID.
     *
     * @param chatId Optional existing chat ID.
     * @param otherUserId The ID of the target participant.
     */
    fun initializeChat(chatId: String?, otherUserId: String) {
        if (!chatId.isNullOrEmpty()) {
            currentChatId = chatId
            loadMessages(chatId)
        } else {
            viewModelScope.launch {
                val result = chatRepository.getOrCreateChatId(otherUserId)
                if (result is Resource.Success && result.data != null) {
                    currentChatId = result.data
                    loadMessages(result.data)
                } else {
                    _messages.postValue(Resource.Error("Sohbet oluşturulamadı"))
                }
            }
        }
    }

    private fun loadMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collectLatest {
                _messages.postValue(it)
            }
        }
    }

    /**
     * Sends a text message to the current chat session.
     * Ensures an active chat document exists before dispatching.
     *
     * @param text The message content.
     * @param otherUserId Fallback targeted user ID to generate a session if missing.
     */
    fun sendMessage(text: String, otherUserId: String) {
        if (text.isBlank()) return
        if (text.length > 1000) {
            _sendState.postValue(Resource.Error("Mesaj karakter sınırı aşıldı!"))
            return
        }
        
        viewModelScope.launch {
            _sendState.postValue(Resource.Loading())
            
            val chatId = currentChatId ?: run {
                val result = chatRepository.getOrCreateChatId(otherUserId)
                if (result is Resource.Success && result.data != null) {
                    currentChatId = result.data
                    loadMessages(result.data)
                    result.data
                } else {
                    null
                }
            }

            if (chatId != null) {
                val sendResult = chatRepository.sendMessage(chatId, text)
                _sendState.postValue(sendResult)
            } else {
                _sendState.postValue(Resource.Error("Sohbet başlatılamadı"))
            }
        }
    }
}
