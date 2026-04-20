package com.example.instagram_clone.presentation.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.instagram_clone.R
import com.example.instagram_clone.domain.model.Post
import com.example.instagram_clone.databinding.ItemPostCardBinding

class FeedAdapter(
    private val currentUserId: String = "",
    private val onLikeClick: (Post) -> Unit = {},
    private val onCommentClick: (Post) -> Unit = {},
    private val onUserClick: (String) -> Unit = {},
    private val onShareClick: (Post) -> Unit = {}
) : ListAdapter<Post, FeedAdapter.PostViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)

        with(holder.binding) {
            val displayUsername = post.username.ifEmpty { post.email.substringBefore("@") }
            postUserEmail.text = displayUsername
            postUserInitial.text = displayUsername.firstOrNull()?.uppercase() ?: "?"
            postCommentUser.text = displayUsername

            if (post.profileImageUrl.isNotEmpty()) {
                postProfileImage.visibility = View.VISIBLE
                Glide.with(root.context)
                    .load(post.profileImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(postProfileImage)
            } else {
                postProfileImage.visibility = View.GONE
            }

            if (post.comment.isNotEmpty()) {
                postComment.text = post.comment
                postComment.visibility = View.VISIBLE
                postCommentUser.visibility = View.VISIBLE
            } else {
                postComment.visibility = View.GONE
                postCommentUser.visibility = View.GONE
            }

            Glide.with(root.context)
                .load(post.downloadUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(postImage)

            postTimeAgo.text = getTimeAgo(post.date?.toDate()?.time ?: System.currentTimeMillis())

            val isLiked = post.likes.contains(currentUserId)
            likeButton.setImageResource(if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like)

            val likeCount = post.likes.size
            if (likeCount > 0) {
                likeCountText.text = "$likeCount beğenme"
                likeCountText.visibility = View.VISIBLE
            } else {
                likeCountText.visibility = View.GONE
            }

            if (post.commentCount > 0) {
                commentCountText.text = "${post.commentCount} yorumun tümünü gör"
                commentCountText.visibility = View.VISIBLE
            } else {
                commentCountText.visibility = View.GONE
            }

            likeButton.setOnClickListener {
                onLikeClick(post)
                val bounceAnim = AnimationUtils.loadAnimation(root.context, R.anim.bounce)
                likeButton.startAnimation(bounceAnim)
            }

            commentButton.setOnClickListener { onCommentClick(post) }
            commentCountText.setOnClickListener { onCommentClick(post) }
            shareButton.setOnClickListener { onShareClick(post) }

            postUserEmail.setOnClickListener {
                if (post.userId.isNotEmpty()) onUserClick(post.userId)
            }
        }
    }

    private fun getTimeAgo(time: Long): String {
        val diff = System.currentTimeMillis() - time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}g önce"
            hours > 0 -> "${hours}sa önce"
            minutes > 0 -> "${minutes}dk önce"
            else -> "Az önce"
        }
    }

    class PostViewHolder(val binding: ItemPostCardBinding) : RecyclerView.ViewHolder(binding.root)
}
