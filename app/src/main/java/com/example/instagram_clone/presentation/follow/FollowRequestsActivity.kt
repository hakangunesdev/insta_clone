package com.example.instagram_clone.presentation.follow

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram_clone.databinding.ActivityFollowRequestsBinding
import com.example.instagram_clone.presentation.search.UserListAdapter
import com.example.instagram_clone.util.Resource

class FollowRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFollowRequestsBinding
    private val viewModel: FollowRequestsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = UserListAdapter(
            showRequestButtons = true,
            onAcceptClick = { viewModel.acceptRequest(it) },
            onRejectClick = { viewModel.rejectRequest(it) }
        )
        binding.requestsList.apply {
            layoutManager = LinearLayoutManager(this@FollowRequestsActivity)
            this.adapter = adapter
        }

        binding.backButton.setOnClickListener { finish() }

        viewModel.requests.observe(this) { result ->
            if (result is Resource.Success) {
                val list = result.data ?: emptyList()
                adapter.submitList(list)
                binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.requestsList.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
        viewModel.actionResult.observe(this) { result ->
            if (result is Resource.Error) Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
        }

        viewModel.loadRequests()
    }
}
