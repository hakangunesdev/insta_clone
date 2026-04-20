package com.example.instagram_clone.domain.repository

import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserById(userId: String): Flow<Resource<User>>
    fun getUserByEmail(email: String): Flow<Resource<User>>
    fun createUserProfile(user: User): Flow<Resource<Unit>>
    fun updateUserProfile(userId: String, fields: Map<String, Any>): Flow<Resource<Unit>>
    fun searchUsers(query: String): Flow<Resource<List<User>>>
    fun getOrCreateCurrentUserProfile(): Flow<Resource<User>>
    fun uploadProfilePhoto(userId: String, imageBytes: ByteArray): Flow<Resource<String>>
}
