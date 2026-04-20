package com.example.instagram_clone.domain.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    @get:com.google.firebase.firestore.PropertyName("isPrivate")
    @set:com.google.firebase.firestore.PropertyName("isPrivate")
    var isPrivate: Boolean = false,
    val createdAt: Timestamp? = null
)
