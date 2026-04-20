package com.example.instagram_clone.presentation.chat.room

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.instagram_clone.R
import com.example.instagram_clone.databinding.ActivityChatRoomBinding
import com.example.instagram_clone.util.Resource

class ChatRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatRoomBinding
    private val viewModel: ChatRoomViewModel by viewModels()
    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private var otherUserId: String = ""

    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_OTHER_USER_ID = "other_user_id"
        const val EXTRA_OTHER_USER_NAME = "other_user_name"
        const val EXTRA_OTHER_USER_IMAGE = "other_user_image"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chatId = intent.getStringExtra(EXTRA_CHAT_ID)
        otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""
        val otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME) ?: "Kullanıcı"
        val otherUserImage = intent.getStringExtra(EXTRA_OTHER_USER_IMAGE) ?: ""

        setupToolbar(otherUserName, otherUserImage)
        setupRecyclerView(otherUserImage, otherUserName)
        setupListeners()
        observeViewModel()

        viewModel.initializeChat(chatId, otherUserId)
    }

    private fun setupToolbar(name: String, imageUrl: String) {
        binding.chatUserName.text = name
        binding.chatInitial.text = name.firstOrNull()?.uppercase() ?: "?"
        
        if (imageUrl.isNotEmpty()) {
            binding.chatAvatar.visibility = android.view.View.VISIBLE
            Glide.with(this).asBitmap().load(imageUrl).into(binding.chatAvatar)
        } else {
            binding.chatAvatar.visibility = android.view.View.GONE
        }
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView(otherUserImage: String, otherUserName: String) {
        val currentUserId = viewModel.getCurrentUserId()
        chatRoomAdapter = ChatRoomAdapter(currentUserId, otherUserImage, otherUserName) { postId ->
            startActivity(
                android.content.Intent(this, com.example.instagram_clone.presentation.feed.SinglePostActivity::class.java).apply {
                    putExtra(com.example.instagram_clone.presentation.feed.SinglePostActivity.EXTRA_POST_ID, postId)
                }
            )
        }
        
        binding.recyclerView.apply {
            val layoutManager = LinearLayoutManager(this@ChatRoomActivity).apply {
                stackFromEnd = true
            }
            this.layoutManager = layoutManager
            adapter = chatRoomAdapter
        }
    }

    private fun setupListeners() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text, otherUserId)
                binding.messageInput.text.clear()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    val msgs = result.data ?: emptyList()
                    chatRoomAdapter.submitList(msgs) {
                        if (msgs.isNotEmpty()) {
                            binding.recyclerView.scrollToPosition(msgs.size - 1)
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {}
            }
        }
        
        viewModel.sendState.observe(this) { result ->
            when (result) {
                is Resource.Error -> Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }
}
