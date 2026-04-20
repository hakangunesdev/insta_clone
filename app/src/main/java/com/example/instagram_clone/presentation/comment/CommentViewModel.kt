package com.example.instagram_clone.presentation.comment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.Comment
import com.example.instagram_clone.data.repository.AuthRepositoryImpl
import com.example.instagram_clone.data.repository.PostRepositoryImpl
import com.example.instagram_clone.domain.repository.AuthRepository
import com.example.instagram_clone.domain.repository.PostRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommentViewModel(
    private val postRepository: PostRepository = PostRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _comments = MutableLiveData<Resource<List<Comment>>>()
    val comments: LiveData<Resource<List<Comment>>> = _comments

    private val _addResult = MutableLiveData<Resource<Unit>>()
    val addResult: LiveData<Resource<Unit>> = _addResult

    fun loadComments(postId: String) {
        viewModelScope.launch {
            postRepository.getComments(postId).collectLatest { _comments.postValue(it) }
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        if (text.length > 300) {
            _addResult.postValue(Resource.Error("Yorum çok uzun (maks 300 karakter)!"))
            return
        }
        viewModelScope.launch {
            postRepository.addComment(postId, text).collectLatest { _addResult.postValue(it) }
        }
    }

    fun getCurrentUserEmail(): String = authRepository.getCurrentUser()?.email ?: ""
}
