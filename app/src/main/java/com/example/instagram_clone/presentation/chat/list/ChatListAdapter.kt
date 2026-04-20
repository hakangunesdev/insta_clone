package com.example.instagram_clone.presentation.chat.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.instagram_clone.R
import com.example.instagram_clone.domain.model.Chat
import com.example.instagram_clone.databinding.ItemChatRowBinding

class ChatListAdapter(
    private val onClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatListAdapter.ChatViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(private val binding: ItemChatRowBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(getItem(position))
                }
            }
        }

        fun bind(chat: Chat) {
            binding.chatUserName.text = chat.otherUserName.ifEmpty { "Kullanıcı" }
            binding.chatLastMessage.text = chat.lastMessage.ifEmpty { "Sohbeti başlat" }

            binding.chatUserInitial.text = chat.otherUserName.firstOrNull()?.uppercase() ?: "?"
            
            if (chat.otherUserProfileImage.isNotEmpty()) {
                binding.chatUserAvatar.visibility = android.view.View.VISIBLE
                Glide.with(itemView.context)
                    .asBitmap()
                    .load(chat.otherUserProfileImage)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.chatUserAvatar)
            } else {
                binding.chatUserAvatar.visibility = android.view.View.GONE
            }
        }
    }
}
