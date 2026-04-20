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

class FollowRequestsViewModel(
    private val followRepository: FollowRepository = FollowRepositoryImpl()
) : ViewModel() {

    private val _requests = MutableLiveData<Resource<List<User>>>()
    val requests: LiveData<Resource<List<User>>> = _requests

    private val _actionResult = MutableLiveData<Resource<Unit>>()
    val actionResult: LiveData<Resource<Unit>> = _actionResult

    fun loadRequests() {
        viewModelScope.launch {
            followRepository.getFollowRequests().collectLatest { _requests.postValue(it) }
        }
    }

    fun acceptRequest(user: User) {
        viewModelScope.launch {
            followRepository.acceptFollowRequest(user.id, user).collectLatest {
                _actionResult.postValue(it)
                loadRequests()
            }
        }
    }

    fun rejectRequest(user: User) {
        viewModelScope.launch {
            followRepository.rejectFollowRequest(user.id).collectLatest {
                _actionResult.postValue(it)
                loadRequests()
            }
        }
    }
}
