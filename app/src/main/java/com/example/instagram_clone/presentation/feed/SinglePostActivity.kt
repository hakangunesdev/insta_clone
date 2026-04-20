package com.example.instagram_clone.presentation.feed

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram_clone.databinding.ActivitySinglePostBinding

class SinglePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySinglePostBinding
    private val viewModel: SinglePostViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySinglePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val postId = intent.getStringExtra(EXTRA_POST_ID)
        if (postId == null) {
            finish()
            return
        }

        binding.backButton.setOnClickListener { finish() }

        setupRecyclerView()
        observeData()

        viewModel.loadPost(postId)
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onLikeClick = { post -> viewModel.toggleLike(post.id) },
            onCommentClick = { post ->
                startActivity(Intent(this, com.example.instagram_clone.presentation.comment.CommentActivity::class.java).apply {
                    putExtra(com.example.instagram_clone.presentation.comment.CommentActivity.EXTRA_POST_ID, post.id)
                })
            },
            onUserClick = { userId ->
                startActivity(Intent(this, com.example.instagram_clone.presentation.profile.ProfileActivity::class.java).apply {
                    putExtra(com.example.instagram_clone.presentation.profile.ProfileActivity.EXTRA_USER_ID, userId)
                })
            },
            onShareClick = { post ->
                startActivity(Intent(this, com.example.instagram_clone.presentation.share.SharePostActivity::class.java).apply {
                    putExtra(com.example.instagram_clone.presentation.share.SharePostActivity.EXTRA_POST_IMAGE_URL, post.downloadUrl)
                    putExtra(com.example.instagram_clone.presentation.share.SharePostActivity.EXTRA_POST_CAPTION, post.comment)
                    putExtra(com.example.instagram_clone.presentation.share.SharePostActivity.EXTRA_POST_USERNAME, post.username.ifEmpty { post.email.substringBefore("@") })
                    putExtra(com.example.instagram_clone.presentation.share.SharePostActivity.EXTRA_POST_ID, post.id)
                })
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SinglePostActivity)
            adapter = feedAdapter
        }
    }

    private fun observeData() {
        viewModel.post.observe(this) { post ->
            if (post != null) {
                feedAdapter.submitList(listOf(post))
            } else {
                Toast.makeText(this, "Gönderi bulunamadı", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}
