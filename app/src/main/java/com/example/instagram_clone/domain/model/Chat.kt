package com.example.instagram_clone.domain.model

import com.google.firebase.Timestamp

/**
 * Represents a chat session between participants.
 * Contains both persistent database fields and runtime UI fields like [otherUserId].
 */
data class Chat(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserProfileImage: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val timestamp: Timestamp? = null
)
