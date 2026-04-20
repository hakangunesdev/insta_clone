package com.example.instagram_clone.presentation.share

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.instagram_clone.R
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.data.repository.ChatRepositoryImpl
import com.example.instagram_clone.data.repository.UserRepositoryImpl
import com.example.instagram_clone.databinding.ActivitySharePostBinding
import com.example.instagram_clone.presentation.search.UserListAdapter
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SharePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySharePostBinding
    private lateinit var userAdapter: UserListAdapter
    private val userRepository = UserRepositoryImpl()
    private val chatRepository = ChatRepositoryImpl()
    private var searchJob: Job? = null
    private var postImageUrl: String = ""
    private var postCaption: String = ""
    private var postId: String = ""

    companion object {
        const val EXTRA_POST_IMAGE_URL = "post_image_url"
        const val EXTRA_POST_CAPTION = "post_caption"
        const val EXTRA_POST_USERNAME = "post_username"
        const val EXTRA_POST_ID = "post_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postImageUrl = intent.getStringExtra(EXTRA_POST_IMAGE_URL) ?: ""
        postCaption = intent.getStringExtra(EXTRA_POST_CAPTION) ?: ""
        postId = intent.getStringExtra(EXTRA_POST_ID) ?: ""
        val postUsername = intent.getStringExtra(EXTRA_POST_USERNAME) ?: ""

        if (postImageUrl.isNotEmpty()) {
            binding.postPreviewImage.visibility = View.VISIBLE
            Glide.with(this).load(postImageUrl).centerCrop().into(binding.postPreviewImage)
        }
        binding.postPreviewCaption.text = if (postCaption.isNotEmpty()) "$postUsername: $postCaption" else "Gönderi paylaş"

        setupRecyclerView()
        setupSearch()
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        userAdapter = UserListAdapter(
            onUserClick = { user -> sharePostToUser(user) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SharePostActivity)
            adapter = userAdapter
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener { text ->
            val query = text.toString().trim()
            searchJob?.cancel()
            if (query.isBlank()) {
                userAdapter.submitList(emptyList())
                return@addTextChangedListener
            }
            searchJob = lifecycleScope.launch {
                delay(300)
                userRepository.searchUsers(query).collect { result ->
                    if (result is Resource.Success) {
                        userAdapter.submitList(result.data ?: emptyList())
                    }
                }
            }
        }
    }

    private fun sharePostToUser(user: User) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val chatResult = chatRepository.getOrCreateChatId(user.id)
            if (chatResult is Resource.Success && chatResult.data != null) {
                val chatId = chatResult.data

                val shareText = if (postCaption.isNotEmpty()) "[Gönderi] $postCaption" else "[Gönderi Paylaşıldı]"

                val sendResult = chatRepository.sendMessage(
                    chatId = chatId, 
                    text = shareText, 
                    postId = postId.ifEmpty { null },
                    postImageUrl = postImageUrl.ifEmpty { null }
                )
                binding.progressBar.visibility = View.GONE

                if (sendResult is Resource.Success) {
                    Toast.makeText(this@SharePostActivity, "${user.username} adlı kullanıcıya başarıyla gönderildi.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@SharePostActivity, "Gönderilemedi", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@SharePostActivity, "Sohbet oluşturulamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
