package com.example.instagram_clone.presentation.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.FollowStatus
import com.example.instagram_clone.domain.model.Post
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.data.repository.AuthRepositoryImpl
import com.example.instagram_clone.data.repository.FollowRepositoryImpl
import com.example.instagram_clone.data.repository.PostRepositoryImpl
import com.example.instagram_clone.data.repository.UserRepositoryImpl
import com.example.instagram_clone.domain.repository.AuthRepository
import com.example.instagram_clone.domain.repository.FollowRepository
import com.example.instagram_clone.domain.repository.PostRepository
import com.example.instagram_clone.domain.repository.UserRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val postRepository: PostRepository = PostRepositoryImpl(),
    private val followRepository: FollowRepository = FollowRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _user = MutableLiveData<Resource<User>>()
    val user: LiveData<Resource<User>> = _user

    private val _posts = MutableLiveData<Resource<List<Post>>>()
    val posts: LiveData<Resource<List<Post>>> = _posts

    private val _followStatus = MutableLiveData<Resource<FollowStatus>>()
    val followStatus: LiveData<Resource<FollowStatus>> = _followStatus

    private val _followersCount = MutableLiveData<Int>(0)
    val followersCount: LiveData<Int> = _followersCount

    private val _followingCount = MutableLiveData<Int>(0)
    val followingCount: LiveData<Int> = _followingCount

    private val _followRequestsCount = MutableLiveData<Int>(0)
    val followRequestsCount: LiveData<Int> = _followRequestsCount

    private val _actionResult = MutableLiveData<Resource<Unit>>()
    val actionResult: LiveData<Resource<Unit>> = _actionResult

    /**
     * Retrieves the current authenticated user's ID.
     */
    fun getCurrentUserId(): String = authRepository.getCurrentUserId()

    /**
     * Checks if the given user ID belongs to the current logged-in user.
     */
    fun isOwnProfile(userId: String): Boolean = userId == getCurrentUserId()

    /**
     * Loads the target user's profile details including posts, follow status, and statistical counts.
     *
     * @param userId The ID of the profile to load.
     */
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            userRepository.getUserById(userId).collectLatest { _user.postValue(it) }
        }
        loadPosts(userId)
        loadCounts(userId)
        if (!isOwnProfile(userId)) {
            loadFollowStatus(userId)
        } else {
            loadFollowRequests()
        }
    }

    /**
     * Toggles the follow state for a target user profile based on its current relationship status.
     * Manages logic for both public (direct follow) and private (send request) accounts.
     *
     * @param targetUser The user entity to follow/unfollow.
     */
    fun toggleFollow(targetUser: User) {
        val status = (_followStatus.value as? Resource.Success)?.data ?: FollowStatus.NONE
        viewModelScope.launch {
            when (status) {
                FollowStatus.NONE -> {
                    if (targetUser.isPrivate) {
                        followRepository.sendFollowRequest(targetUser.id, targetUser).collectLatest { _actionResult.postValue(it) }
                    } else {
                        followRepository.followUser(targetUser.id, targetUser).collectLatest { _actionResult.postValue(it) }
                    }
                }
                FollowStatus.FOLLOWING -> {
                    followRepository.unfollowUser(targetUser.id).collectLatest { _actionResult.postValue(it) }
                }
                FollowStatus.REQUESTED -> {
                    followRepository.cancelFollowRequest(targetUser.id).collectLatest { _actionResult.postValue(it) }
                }
            }
            loadFollowStatus(targetUser.id)
            loadCounts(targetUser.id)
        }
    }

    private fun loadPosts(userId: String) {
        viewModelScope.launch {
            postRepository.getPostsByUserId(userId).collectLatest { _posts.postValue(it) }
        }
    }

    private fun loadCounts(userId: String) {
        viewModelScope.launch {
            followRepository.getFollowersCount(userId).collectLatest { result ->
                if (result is Resource.Success) _followersCount.postValue(result.data ?: 0)
            }
        }
        viewModelScope.launch {
            followRepository.getFollowingCount(userId).collectLatest { result ->
                if (result is Resource.Success) _followingCount.postValue(result.data ?: 0)
            }
        }
    }

    private fun loadFollowStatus(userId: String) {
        viewModelScope.launch {
            followRepository.getFollowStatus(userId).collectLatest { _followStatus.postValue(it) }
        }
    }

    private fun loadFollowRequests() {
        viewModelScope.launch {
            followRepository.getFollowRequests().collectLatest { result ->
                if (result is Resource.Success) _followRequestsCount.postValue(result.data?.size ?: 0)
            }
        }
    }
}
