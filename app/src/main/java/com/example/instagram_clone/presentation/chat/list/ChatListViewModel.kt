package com.example.instagram_clone.presentation.chat.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.Chat
import com.example.instagram_clone.data.repository.ChatRepositoryImpl
import com.example.instagram_clone.data.repository.UserRepositoryImpl
import com.example.instagram_clone.domain.repository.ChatRepository
import com.example.instagram_clone.domain.repository.UserRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatRepository: ChatRepository = ChatRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _chats = MutableLiveData<Resource<List<Chat>>>()
    val chats: LiveData<Resource<List<Chat>>> = _chats

    fun loadChats() {
        viewModelScope.launch {
            chatRepository.getConversations().collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val chats = resource.data ?: emptyList()
                        val enrichedChats = chats.map { chat ->
                            val userResult = userRepository.getUserById(chat.otherUserId).firstOrNull { it !is Resource.Loading }
                            if (userResult is Resource.Success && userResult.data != null) {
                                chat.copy(
                                    otherUserName = userResult.data.username,
                                    otherUserProfileImage = userResult.data.profileImageUrl
                                )
                            } else {
                                chat
                            }
                        } ?: emptyList()
                        _chats.postValue(Resource.Success(enrichedChats))
                    }
                    is Resource.Loading -> _chats.postValue(Resource.Loading())
                    is Resource.Error -> _chats.postValue(Resource.Error(resource.message ?: "Hata"))
                }
            }
        }
    }
}
