package com.example.instagram_clone.presentation.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram_clone.databinding.ActivitySearchBinding
import com.example.instagram_clone.presentation.profile.ProfileActivity
import com.example.instagram_clone.util.Resource

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var userAdapter: UserListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userAdapter = UserListAdapter(onUserClick = { user ->
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra(ProfileActivity.EXTRA_USER_ID, user.id)
            })
        })
        binding.searchResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = userAdapter
        }

        binding.backButton.setOnClickListener { finish() }
        binding.searchInput.addTextChangedListener { text ->
            viewModel.searchUsers(text.toString().trim())
        }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchUsers(binding.searchInput.text.toString().trim())
                true
            } else false
        }

        viewModel.users.observe(this) { result ->
            when (result) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val users = result.data ?: emptyList()
                    userAdapter.submitList(users)
                    binding.emptyState.visibility = if (users.isEmpty() && binding.searchInput.text.isNullOrBlank()) View.VISIBLE else View.GONE
                    binding.searchResults.visibility = if (users.isNotEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
}
