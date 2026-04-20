package com.example.instagram_clone.presentation.feed

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram_clone.databinding.ActivityFeedBinding
import com.example.instagram_clone.presentation.auth.AuthActivity
import com.example.instagram_clone.presentation.comment.CommentActivity
import com.example.instagram_clone.presentation.profile.ProfileActivity
import com.example.instagram_clone.presentation.search.SearchActivity
import com.example.instagram_clone.presentation.upload.UploadActivity
import com.example.instagram_clone.util.Resource

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()
        observePosts()
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onLikeClick = { post ->
                if (post.id.isNotEmpty()) viewModel.toggleLike(post.id)
            },
            onCommentClick = { post ->
                if (post.id.isNotEmpty()) {
                    startActivity(Intent(this, CommentActivity::class.java).apply {
                        putExtra(CommentActivity.EXTRA_POST_ID, post.id)
                    })
                }
            },
            onUserClick = { userId ->
                startActivity(Intent(this, ProfileActivity::class.java).apply {
                    putExtra(ProfileActivity.EXTRA_USER_ID, userId)
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
            layoutManager = LinearLayoutManager(this@FeedActivity)
            adapter = feedAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(5)
        }
    }

    private fun setupClickListeners() {
        binding.addPostButton.setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }
        binding.dmButton.setOnClickListener {
            startActivity(Intent(this, com.example.instagram_clone.presentation.chat.list.ChatListActivity::class.java))
        }
        binding.searchButton.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.logoutButton.setOnClickListener {
            viewModel.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light,
            android.R.color.holo_purple
        )
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadPosts() }
    }

    private fun observePosts() {
        viewModel.posts.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) binding.progressBar.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    val posts = result.data ?: emptyList()
                    if (posts.isEmpty()) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        feedAdapter.submitList(posts)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
