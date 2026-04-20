package com.example.instagram_clone.presentation.comment

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram_clone.databinding.ActivityCommentBinding
import com.example.instagram_clone.util.Resource

class CommentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentBinding
    private val viewModel: CommentViewModel by viewModels()
    private lateinit var commentAdapter: CommentAdapter
    private var postId: String = ""

    companion object {
        const val EXTRA_POST_ID = "post_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getStringExtra(EXTRA_POST_ID) ?: run { finish(); return }

        commentAdapter = CommentAdapter()
        binding.commentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CommentActivity)
            adapter = commentAdapter
        }

        val email = viewModel.getCurrentUserEmail()
        binding.commentUserInitial.text = if (email.isNotEmpty()) email.first().uppercase() else "?"

        binding.backButton.setOnClickListener { finish() }
        binding.sendButton.setOnClickListener {
            val text = binding.commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.addComment(postId, text)
                binding.commentInput.text?.clear()
            }
        }

        viewModel.comments.observe(this) { result ->
            if (result is Resource.Success) {
                commentAdapter.submitList(result.data)
                if ((result.data?.size ?: 0) > 0) {
                    binding.commentsRecyclerView.scrollToPosition((result.data?.size ?: 1) - 1)
                }
            }
        }
        viewModel.addResult.observe(this) { result ->
            if (result is Resource.Error) Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
        }

        viewModel.loadComments(postId)
    }
}
