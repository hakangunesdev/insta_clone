package com.example.instagram_clone.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.data.repository.AuthRepositoryImpl
import com.example.instagram_clone.domain.repository.AuthRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _authState = MutableLiveData<Resource<Unit>>()
    val authState: LiveData<Resource<Unit>> = _authState

    /**
     * Attempts to authenticate the user with the provided credentials.
     * Posts the state to [authState].
     *
     * @param email User's email address.
     * @param password User's password.
     */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = Resource.Error("Lütfen E-mail ve Şifre girin!")
            return
        }
        viewModelScope.launch {
            authRepository.signIn(email, password).collectLatest { result ->
                _authState.postValue(result)
            }
        }
    }

    /**
     * Registers a new user account if credentials are valid.
     * Posts the state to [authState].
     *
     * @param email New user's email.
     * @param password New user's password.
     * @param username New user's initial username.
     */
    fun signUp(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = Resource.Error("Lütfen E-mail ve Şifre girin!")
            return
        }
        if (username.isBlank()) {
            _authState.value = Resource.Error("Lütfen kullanıcı adı girin!")
            return
        }
        viewModelScope.launch {
            authRepository.signUp(email, password, username).collectLatest { result ->
                _authState.postValue(result)
            }
        }
    }

    /**
     * Checks if a persistent user session exists.
     *
     * @return true if logged in, false otherwise.
     */
    fun isUserLoggedIn(): Boolean = authRepository.getCurrentUser() != null
}
