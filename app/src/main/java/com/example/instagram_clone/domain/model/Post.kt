package com.example.instagram_clone.domain.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val comment: String = "",
    val downloadUrl: String = "",
    val date: Timestamp? = null,
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0
)
