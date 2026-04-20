package com.example.instagram_clone.presentation.chat.room

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.instagram_clone.R
import com.example.instagram_clone.domain.model.Message

class ChatRoomAdapter(
    private val currentUserId: String,
    private val otherUserImage: String,
    private val otherUserName: String,
    private val onPostClick: (String) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2

        private val DiffCallback = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message, otherUserImage, otherUserName)
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMsg: TextView = itemView.findViewById(R.id.textMessageSent)
        private val btnGoToPost: View? = itemView.findViewById(R.id.btnGoToPost)
        private val postImage: ImageView? = itemView.findViewById(R.id.postImage)
        
        fun bind(message: Message) {
            textMsg.text = message.text
            
            if (message.postImageUrl.isNotEmpty() && postImage != null) {
                postImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.postImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(postImage)
            } else {
                postImage?.visibility = View.GONE
            }
            
            if (message.postId.isNotEmpty() && btnGoToPost != null) {
                btnGoToPost.visibility = View.VISIBLE
                btnGoToPost.setOnClickListener { onPostClick(message.postId) }
            } else {
                btnGoToPost?.visibility = View.GONE
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMsg: TextView = itemView.findViewById(R.id.textMessageReceived)
        private val avatar: ImageView = itemView.findViewById(R.id.avatarMessageReceived)
        private val messageInitial: TextView? = itemView.findViewById(R.id.messageInitial)
        private val btnGoToPost: View? = itemView.findViewById(R.id.btnGoToPost)
        private val postImage: ImageView? = itemView.findViewById(R.id.postImage)
        
        fun bind(message: Message, imageUrl: String, userName: String) {
            textMsg.text = message.text
            messageInitial?.text = userName.firstOrNull()?.uppercase() ?: "?"
            
            if (imageUrl.isNotEmpty()) {
                avatar.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .asBitmap()
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(avatar)
            } else {
                avatar.visibility = View.GONE
            }
            
            if (message.postImageUrl.isNotEmpty() && postImage != null) {
                postImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.postImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(postImage)
            } else {
                postImage?.visibility = View.GONE
            }
            
            if (message.postId.isNotEmpty() && btnGoToPost != null) {
                btnGoToPost.visibility = View.VISIBLE
                btnGoToPost.setOnClickListener { onPostClick(message.postId) }
            } else {
                btnGoToPost?.visibility = View.GONE
            }
        }
    }
}
