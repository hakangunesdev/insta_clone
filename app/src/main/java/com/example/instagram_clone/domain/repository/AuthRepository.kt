package com.example.instagram_clone.domain.repository

import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signIn(email: String, password: String): Flow<Resource<Unit>>
    fun signUp(email: String, password: String, username: String): Flow<Resource<Unit>>
    fun signOut()
    fun getCurrentUser(): User?
    fun getCurrentUserId(): String
}
