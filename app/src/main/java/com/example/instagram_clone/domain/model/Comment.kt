package com.example.instagram_clone.domain.model

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val text: String = "",
    val date: Timestamp? = null
)
