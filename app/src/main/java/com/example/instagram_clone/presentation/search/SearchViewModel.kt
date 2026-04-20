package com.example.instagram_clone.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.data.repository.UserRepositoryImpl
import com.example.instagram_clone.domain.repository.UserRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchViewModel(
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _users = MutableLiveData<Resource<List<User>>>()
    val users: LiveData<Resource<List<User>>> = _users

    private var searchJob: Job? = null

    /**
     * Searches for users based on a query string.
     * Incorporates a debounce delay to limit rapid API calls during typing.
     *
     * @param query The search text input by the user.
     */
    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _users.value = Resource.Success(emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            userRepository.searchUsers(query).collectLatest { _users.postValue(it) }
        }
    }
}
