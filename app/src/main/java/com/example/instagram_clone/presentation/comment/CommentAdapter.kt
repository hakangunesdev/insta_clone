package com.example.instagram_clone.presentation.comment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.instagram_clone.domain.model.Comment
import com.example.instagram_clone.databinding.ItemCommentRowBinding

class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    class CommentViewHolder(val binding: ItemCommentRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(ItemCommentRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        with(holder.binding) {
            commentUsername.text = comment.username.ifEmpty { "kullanıcı" }
            commentText.text = comment.text
            commentInitial.text = comment.username.firstOrNull()?.uppercase() ?: "?"
            
            if (comment.profileImageUrl.isNotEmpty()) {
                commentProfileImage.visibility = android.view.View.VISIBLE
                com.bumptech.glide.Glide.with(root.context).load(comment.profileImageUrl).centerCrop().into(commentProfileImage)
            } else {
                commentProfileImage.visibility = android.view.View.GONE
            }

            val diff = System.currentTimeMillis() - (comment.date?.toDate()?.time ?: System.currentTimeMillis())
            val minutes = diff / 60000
            val hours = minutes / 60
            val days = hours / 24
            commentTime.text = when {
                days > 0 -> "${days}g"
                hours > 0 -> "${hours}sa"
                minutes > 0 -> "${minutes}dk"
                else -> "Az önce"
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = oldItem == newItem
    }
}
