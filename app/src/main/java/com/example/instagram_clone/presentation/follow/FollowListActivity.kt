package com.example.instagram_clone.presentation.follow

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram_clone.databinding.ActivityFollowListBinding
import com.example.instagram_clone.presentation.profile.ProfileActivity
import com.example.instagram_clone.presentation.search.UserListAdapter
import com.example.instagram_clone.util.Resource
import com.google.android.material.tabs.TabLayout

class FollowListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFollowListBinding
    private val viewModel: FollowListViewModel by viewModels()
    private lateinit var userAdapter: UserListAdapter
    private var userId: String = ""

    companion object {
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_TAB = "tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra(EXTRA_USER_ID) ?: run { finish(); return }
        val initialTab = intent.getIntExtra(EXTRA_TAB, 0)

        userAdapter = UserListAdapter(onUserClick = { user ->
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra(ProfileActivity.EXTRA_USER_ID, user.id)
            })
        })
        binding.userList.apply {
            layoutManager = LinearLayoutManager(this@FollowListActivity)
            adapter = userAdapter
        }

        binding.backButton.setOnClickListener { finish() }

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Takipçiler"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Takip Edilenler"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) viewModel.loadFollowers(userId)
                else viewModel.loadFollowing(userId)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewModel.users.observe(this) { result ->
            if (result is Resource.Success) {
                userAdapter.submitList(result.data ?: emptyList())
                binding.toolbarTitle.text = if (binding.tabLayout.selectedTabPosition == 0) "Takipçiler" else "Takip Edilenler"
            }
        }

        binding.tabLayout.getTabAt(initialTab)?.select()
        if (initialTab == 0) viewModel.loadFollowers(userId)
        else viewModel.loadFollowing(userId)
    }
}
