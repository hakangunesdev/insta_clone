package com.example.instagram_clone.domain.model

/**
 * Represents the current following state relationship between a user and a target profile.
 */
enum class FollowStatus {
    /** Target account is not being followed. */
    NONE,
    
    /** Target account is actively being followed. */
    FOLLOWING,
    
    /** Target account is private and a follow request is pending. */
    REQUESTED
}
