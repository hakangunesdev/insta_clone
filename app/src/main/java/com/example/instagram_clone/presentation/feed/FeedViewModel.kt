package com.example.instagram_clone.presentation.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.Post
import com.example.instagram_clone.data.repository.AuthRepositoryImpl
import com.example.instagram_clone.data.repository.PostRepositoryImpl
import com.example.instagram_clone.data.repository.UserRepositoryImpl
import com.example.instagram_clone.data.repository.FollowRepositoryImpl
import com.example.instagram_clone.domain.repository.AuthRepository
import com.example.instagram_clone.domain.repository.PostRepository
import com.example.instagram_clone.domain.repository.UserRepository
import com.example.instagram_clone.domain.repository.FollowRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FeedViewModel(
    private val postRepository: PostRepository = PostRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val followRepository: FollowRepository = FollowRepositoryImpl()
) : ViewModel() {

    private val _posts = MutableLiveData<Resource<List<Post>>>()
    val posts: LiveData<Resource<List<Post>>> = _posts

    init {
        ensureProfileExists()
        loadPosts()
    }

    /**
     * Loads the posts from the repository that belong to the current authenticated user
     * and the users they are following.
     */
    fun loadPosts() {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId()
            combine(
                followRepository.getFollowing(currentUserId),
                postRepository.getPosts()
            ) { followingResult, postsResult ->
                if (followingResult is Resource.Success && postsResult is Resource.Success) {
                    val followingIds = followingResult.data?.map { it.id } ?: emptyList()
                    val eligibleIds = followingIds + currentUserId
                    
                    val filteredPosts = postsResult.data?.filter { post ->
                        eligibleIds.contains(post.userId)
                    } ?: emptyList()
                    
                    Resource.Success(filteredPosts)
                } else if (postsResult is Resource.Loading || followingResult is Resource.Loading) {
                    Resource.Loading()
                } else {
                    Resource.Error(postsResult.message ?: followingResult.message ?: "Beklenmeyen Hata")
                }
            }.collectLatest { finalResult ->
                _posts.postValue(finalResult)
            }
        }
    }

    /**
     * Toggles the like status of a specific post for the current user.
     *
     * @param postId The ID of the post to like or unlike.
     */
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            postRepository.toggleLike(postId).collectLatest { }
        }
    }

    /**
     * Signs out the current user session.
     */
    fun signOut() {
        authRepository.signOut()
    }

    /**
     * Retrieves the current authenticated user's ID.
     */
    fun getCurrentUserId(): String {
        return authRepository.getCurrentUser()?.id ?: ""
    }

    /**
     * Retrieves the current authenticated user's email.
     */
    fun getCurrentUserEmail(): String {
        return authRepository.getCurrentUser()?.email ?: ""
    }

    /**
     * Legacy support: ensures an explicitly created Firestore profile exists for older accounts.
     */
    private fun ensureProfileExists() {
        viewModelScope.launch {
            try {
                userRepository.getOrCreateCurrentUserProfile().collectLatest { }
            } catch (_: Exception) { }
        }
    }
}
