package com.example.instagram_clone.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.instagram_clone.R
import com.example.instagram_clone.domain.model.FollowStatus
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.databinding.ActivityProfileBinding
import com.example.instagram_clone.presentation.auth.AuthActivity
import com.example.instagram_clone.presentation.follow.FollowListActivity
import com.example.instagram_clone.presentation.follow.FollowRequestsActivity
import com.example.instagram_clone.util.Resource

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var gridAdapter: ProfileGridAdapter
    private var userId: String = ""
    private var currentUser: User? = null

    companion object {
        const val EXTRA_USER_ID = "user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra(EXTRA_USER_ID) ?: viewModel.getCurrentUserId()

        setupGrid()
        setupClickListeners()
        observeData()
        viewModel.loadProfile(userId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile(userId)
    }

    private fun setupGrid() {
        gridAdapter = ProfileGridAdapter()
        binding.postsGrid.apply {
            layoutManager = GridLayoutManager(this@ProfileActivity, 3)
            adapter = gridAdapter
            setItemViewCacheSize(5)
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.followersSection.setOnClickListener {
            startActivity(Intent(this, FollowListActivity::class.java).apply {
                putExtra(FollowListActivity.EXTRA_USER_ID, userId)
                putExtra(FollowListActivity.EXTRA_TAB, 0)
            })
        }

        binding.followingSection.setOnClickListener {
            startActivity(Intent(this, FollowListActivity::class.java).apply {
                putExtra(FollowListActivity.EXTRA_USER_ID, userId)
                putExtra(FollowListActivity.EXTRA_TAB, 1)
            })
        }

        binding.followRequestsBanner.setOnClickListener {
            startActivity(Intent(this, FollowRequestsActivity::class.java))
        }

        binding.actionButton.setOnClickListener {
            if (viewModel.isOwnProfile(userId)) {
                startActivity(Intent(this, EditProfileActivity::class.java))
            } else {
                currentUser?.let { viewModel.toggleFollow(it) }
            }
        }

        binding.messageButton.setOnClickListener {
            currentUser?.let { user ->
                startActivity(Intent(this, com.example.instagram_clone.presentation.chat.room.ChatRoomActivity::class.java).apply {
                    putExtra(com.example.instagram_clone.presentation.chat.room.ChatRoomActivity.EXTRA_OTHER_USER_ID, user.id)
                    putExtra(com.example.instagram_clone.presentation.chat.room.ChatRoomActivity.EXTRA_OTHER_USER_NAME, user.displayName.ifEmpty { user.username })
                    putExtra(com.example.instagram_clone.presentation.chat.room.ChatRoomActivity.EXTRA_OTHER_USER_IMAGE, user.profileImageUrl)
                })
            }
        }

        binding.settingsButton.setOnClickListener {
            com.example.instagram_clone.data.repository.AuthRepositoryImpl().signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finishAffinity()
        }
    }

    private fun observeData() {
        viewModel.user.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    val user = result.data ?: return@observe
                    currentUser = user
                    binding.toolbarUsername.text = user.username
                    binding.displayName.text = user.displayName.ifEmpty { user.username }
                    binding.profileInitial.text = user.username.firstOrNull()?.uppercase() ?: "?"

                    if (user.bio.isNotEmpty()) {
                        binding.bioText.text = user.bio
                        binding.bioText.visibility = View.VISIBLE
                    } else {
                        binding.bioText.visibility = View.GONE
                    }

                    if (user.profileImageUrl.isNotEmpty()) {
                        binding.profileImage.visibility = View.VISIBLE
                        Glide.with(this).load(user.profileImageUrl).circleCrop().into(binding.profileImage)
                    }

                    if (user.isPrivate) binding.lockIcon.visibility = View.VISIBLE

                    if (viewModel.isOwnProfile(userId)) {
                        binding.actionButton.text = "Profili Düzenle"
                        binding.actionButton.setTextColor(getColor(R.color.text_primary))
                        binding.actionButton.setBackgroundColor(getColor(R.color.input_background))
                        binding.settingsButton.visibility = View.VISIBLE
                        binding.messageButton.visibility = View.GONE
                    } else {
                        binding.messageButton.visibility = View.VISIBLE
                        binding.settingsButton.visibility = View.GONE
                    }
                    updatePrivacyUI()
                }
                is Resource.Error -> {
                    if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is Resource.Loading -> {}
            }
        }

        viewModel.posts.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    val posts = result.data ?: emptyList()
                    binding.postCount.text = posts.size.toString()
                    gridAdapter.submitList(posts) {
                        updatePrivacyUI(posts.size)
                    }
                }
                else -> {}
            }
        }

        viewModel.followStatus.observe(this) { result ->
            if (result is Resource.Success) {
                updateFollowButton(result.data ?: FollowStatus.NONE)
                updatePrivacyUI()
            }
        }

        viewModel.followersCount.observe(this) { binding.followersCount.text = it.toString() }
        viewModel.followingCount.observe(this) { binding.followingCount.text = it.toString() }
        viewModel.followRequestsCount.observe(this) { count ->
            if (viewModel.isOwnProfile(userId) && count > 0) {
                binding.followRequestsBanner.visibility = View.VISIBLE
                binding.followRequestsCount.text = count.toString()
            } else {
                binding.followRequestsBanner.visibility = View.GONE
            }
        }
    }

    private fun updateFollowButton(status: FollowStatus) {
        when (status) {
            FollowStatus.NONE -> {
                binding.actionButton.text = "Takip Et"
                binding.actionButton.setTextColor(getColor(R.color.white))
                binding.actionButton.setBackgroundColor(getColor(R.color.instagram_blue))
            }
            FollowStatus.FOLLOWING -> {
                binding.actionButton.text = "Takip Ediliyor"
                binding.actionButton.setTextColor(getColor(R.color.text_primary))
                binding.actionButton.setBackgroundColor(getColor(R.color.input_background))
            }
            FollowStatus.REQUESTED -> {
                binding.actionButton.text = "İstek Gönderildi"
                binding.actionButton.setTextColor(getColor(R.color.text_primary))
                binding.actionButton.setBackgroundColor(getColor(R.color.input_background))
            }
        }
    }

    private fun updatePrivacyUI(explicitSize: Int? = null) {
        val user = currentUser
        val status = (viewModel.followStatus.value as? Resource.Success)?.data ?: FollowStatus.NONE
        val isOwn = viewModel.isOwnProfile(userId)
        val postsCount = explicitSize ?: gridAdapter.currentList.size

        if (user != null && user.isPrivate && !isOwn && status != FollowStatus.FOLLOWING) {
            binding.privateOverlay.visibility = View.VISIBLE
            binding.postsGrid.visibility = View.GONE
            binding.emptyPosts.visibility = View.GONE
        } else {
            binding.privateOverlay.visibility = View.GONE
            if (postsCount == 0) {
                binding.emptyPosts.visibility = View.VISIBLE
                binding.postsGrid.visibility = View.GONE
            } else {
                binding.emptyPosts.visibility = View.GONE
                binding.postsGrid.visibility = View.VISIBLE
            }
        }
    }
}
