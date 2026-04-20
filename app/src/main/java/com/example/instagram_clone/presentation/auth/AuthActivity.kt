package com.example.instagram_clone.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.instagram_clone.R
import com.example.instagram_clone.databinding.ActivityMainBinding
import com.example.instagram_clone.presentation.feed.FeedActivity
import com.example.instagram_clone.util.Resource

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AuthViewModel by viewModels()
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (viewModel.isUserLoggedIn()) {
            navigateToFeed()
            return
        }

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        binding.signInButton.setOnClickListener {
            if (isSignUpMode) {
                val email = binding.userEmailText.text.toString().trim()
                val password = binding.passwordText.text.toString().trim()
                val username = binding.usernameText.text.toString().trim()
                viewModel.signUp(email, password, username)
            } else {
                val email = binding.userEmailText.text.toString().trim()
                val password = binding.passwordText.text.toString().trim()
                viewModel.signIn(email, password)
            }
        }

        binding.signUpButton.setOnClickListener {
            if (isSignUpMode) {
                isSignUpMode = false
                binding.usernameInputLayout.visibility = View.GONE
                binding.signInButton.text = getString(R.string.btn_sign_in)
                binding.signUpButton.text = getString(R.string.create_account)
            } else {
                isSignUpMode = true
                binding.usernameInputLayout.visibility = View.VISIBLE
                binding.signInButton.text = getString(R.string.create_account)
                binding.signUpButton.text = getString(R.string.back_to_login)
            }
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(this) { result ->
            when (result) {
                is Resource.Loading -> setLoadingState(true)
                is Resource.Success -> {
                    setLoadingState(false)
                    Toast.makeText(this, "Hoş geldiniz!", Toast.LENGTH_SHORT).show()
                    navigateToFeed()
                }
                is Resource.Error -> {
                    setLoadingState(false)
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !isLoading
        binding.signUpButton.isEnabled = !isLoading
        binding.userEmailText.isEnabled = !isLoading
        binding.passwordText.isEnabled = !isLoading
        binding.signInButton.alpha = if (isLoading) 0.6f else 1.0f
        binding.signUpButton.alpha = if (isLoading) 0.6f else 1.0f
    }

    private fun navigateToFeed() {
        startActivity(Intent(this, FeedActivity::class.java))
        finish()
    }
}
