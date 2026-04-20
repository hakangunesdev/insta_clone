package com.example.instagram_clone.presentation.chat.list

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram_clone.databinding.ActivityChatListBinding
import com.example.instagram_clone.presentation.chat.room.ChatRoomActivity
import com.example.instagram_clone.util.Resource

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var chatListAdapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        viewModel.loadChats()
    }

    private fun setupRecyclerView() {
        chatListAdapter = ChatListAdapter { chat ->
            val intent = Intent(this, ChatRoomActivity::class.java).apply {
                putExtra(ChatRoomActivity.EXTRA_CHAT_ID, chat.id)
                putExtra(ChatRoomActivity.EXTRA_OTHER_USER_ID, chat.otherUserId)
                putExtra(ChatRoomActivity.EXTRA_OTHER_USER_NAME, chat.otherUserName)
                putExtra(ChatRoomActivity.EXTRA_OTHER_USER_IMAGE, chat.otherUserProfileImage)
            }
            startActivity(intent)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatListActivity)
            adapter = chatListAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(5)
        }
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.newChatButton.setOnClickListener {
            startActivity(android.content.Intent(this, com.example.instagram_clone.presentation.search.SearchActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.chats.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val chats = result.data ?: emptyList()
                    if (chats.isEmpty()) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        chatListAdapter.submitList(chats)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
