package com.example.instagram_clone.domain.repository


import com.example.instagram_clone.domain.model.Comment
import com.example.instagram_clone.domain.model.Post
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPosts(): Flow<Resource<List<Post>>>
    fun getPostsByUserId(userId: String): Flow<Resource<List<Post>>>
    fun uploadPost(imageBytes: ByteArray, comment: String): Flow<Resource<Unit>>
    fun toggleLike(postId: String): Flow<Resource<Unit>>
    fun getComments(postId: String): Flow<Resource<List<Comment>>>
    fun addComment(postId: String, text: String): Flow<Resource<Unit>>
    fun getPostById(postId: String): Flow<Resource<Post>>
    suspend fun updateProfileImageInPosts(userId: String, imageUrl: String)
}
