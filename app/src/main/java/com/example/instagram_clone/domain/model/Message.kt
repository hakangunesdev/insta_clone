package com.example.instagram_clone.domain.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val postId: String = "",
    val postImageUrl: String = ""
)
