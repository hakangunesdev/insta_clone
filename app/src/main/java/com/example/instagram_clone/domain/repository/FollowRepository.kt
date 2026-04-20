package com.example.instagram_clone.domain.repository

import com.example.instagram_clone.domain.model.FollowStatus
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.Flow

interface FollowRepository {
    fun followUser(targetUserId: String, targetUser: User): Flow<Resource<Unit>>
    fun unfollowUser(targetUserId: String): Flow<Resource<Unit>>
    fun sendFollowRequest(targetUserId: String, targetUser: User): Flow<Resource<Unit>>
    fun acceptFollowRequest(requesterId: String, requesterUser: User): Flow<Resource<Unit>>
    fun rejectFollowRequest(requesterId: String): Flow<Resource<Unit>>
    fun cancelFollowRequest(targetUserId: String): Flow<Resource<Unit>>
    fun getFollowers(userId: String): Flow<Resource<List<User>>>
    fun getFollowing(userId: String): Flow<Resource<List<User>>>
    fun getFollowRequests(): Flow<Resource<List<User>>>
    fun getFollowStatus(targetUserId: String): Flow<Resource<FollowStatus>>
    fun getFollowersCount(userId: String): Flow<Resource<Int>>
    fun getFollowingCount(userId: String): Flow<Resource<Int>>
}
