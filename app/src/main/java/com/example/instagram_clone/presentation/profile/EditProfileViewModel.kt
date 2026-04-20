package com.example.instagram_clone.presentation.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.data.repository.AuthRepositoryImpl
import com.example.instagram_clone.data.repository.PostRepositoryImpl
import com.example.instagram_clone.data.repository.UserRepositoryImpl
import com.example.instagram_clone.domain.repository.AuthRepository
import com.example.instagram_clone.domain.repository.PostRepository
import com.example.instagram_clone.domain.repository.UserRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.util.UUID

class EditProfileViewModel(
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val postRepository: PostRepository = PostRepositoryImpl()
) : ViewModel() {

    private val _user = MutableLiveData<Resource<User>>()
    val user: LiveData<Resource<User>> = _user

    private val _saveResult = MutableLiveData<Resource<Unit>>()
    val saveResult: LiveData<Resource<Unit>> = _saveResult

    private val _photoUploadResult = MutableLiveData<Resource<String>>()
    val photoUploadResult: LiveData<Resource<String>> = _photoUploadResult

    private val uid = authRepository.getCurrentUserId()

    fun loadProfile() {
        viewModelScope.launch {
            userRepository.getUserById(uid).collectLatest { _user.postValue(it) }
        }
    }

    fun saveProfile(username: String, displayName: String, bio: String, isPrivate: Boolean) {
        if (username.length > 30 || displayName.length > 50 || bio.length > 150) {
            _saveResult.postValue(Resource.Error("Karakter sınırı aşıldı!"))
            return
        }
        
        val fields = mapOf<String, Any>(
            "username" to username.lowercase().trim(),
            "displayName" to displayName.trim(),
            "bio" to bio.trim(),
            "isPrivate" to isPrivate
        )
        viewModelScope.launch {
            userRepository.updateUserProfile(uid, fields).collectLatest { _saveResult.postValue(it) }
        }
    }

    fun uploadProfilePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _photoUploadResult.value = Resource.Loading()
            val imageBytes = withContext(Dispatchers.IO) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                baos.toByteArray()
            }
            
            userRepository.uploadProfilePhoto(uid, imageBytes).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        val url = result.data ?: return@collectLatest
                        userRepository.updateUserProfile(uid, mapOf("profileImageUrl" to url)).collectLatest { updateResult ->
                            if (updateResult is Resource.Success) {
                                postRepository.updateProfileImageInPosts(uid, url)
                            }
                            _photoUploadResult.postValue(Resource.Success(url))
                        }
                    }
                    is Resource.Loading -> _photoUploadResult.value = Resource.Loading()
                    is Resource.Error -> _photoUploadResult.postValue(Resource.Error(result.message ?: "Yükleme başarısız"))
                }
            }
        }
    }
}
