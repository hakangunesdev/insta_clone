package com.example.instagram_clone.presentation.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.Post
import com.example.instagram_clone.data.repository.AuthRepositoryImpl
import com.example.instagram_clone.data.repository.PostRepositoryImpl
import kotlinx.coroutines.launch

class SinglePostViewModel : ViewModel() {
    private val postRepository = PostRepositoryImpl()
    private val authRepository = AuthRepositoryImpl()

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun getCurrentUserId(): String = authRepository.getCurrentUserId()

    fun loadPost(postId: String) {
        _loading.value = true
        viewModelScope.launch {
            postRepository.getPostById(postId).collect { resource ->
                if (resource is com.example.instagram_clone.util.Resource.Success) {
                    _post.postValue(resource.data)
                }
                _loading.postValue(false)
            }
        }
    }

    /**
     * Toggles the like status of the specified post.
     * Reloads the post data sequentially after the operation.
     *
     * @param postId The ID of the post to like/unlike.
     */
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            postRepository.toggleLike(postId).collect { }
            loadPost(postId)
        }
    }
}
