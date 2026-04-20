package com.example.instagram_clone.presentation.follow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.data.repository.FollowRepositoryImpl
import com.example.instagram_clone.domain.repository.FollowRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FollowListViewModel(
    private val followRepository: FollowRepository = FollowRepositoryImpl()
) : ViewModel() {

    private val _users = MutableLiveData<Resource<List<User>>>()
    val users: LiveData<Resource<List<User>>> = _users

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            followRepository.getFollowers(userId).collectLatest { _users.postValue(it) }
        }
    }

    fun loadFollowing(userId: String) {
        viewModelScope.launch {
            followRepository.getFollowing(userId).collectLatest { _users.postValue(it) }
        }
    }
}
