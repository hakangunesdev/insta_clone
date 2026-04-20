package com.example.instagram_clone.presentation.profile

import android.content.Intent
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.instagram_clone.databinding.ActivityEditProfileBinding
import com.example.instagram_clone.util.Resource

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                    viewModel.uploadProfilePhoto(bitmap)
                    binding.profileImage.setImageBitmap(bitmap)
                    binding.profileImage.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.photoContainer.setOnClickListener {
            galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
        }
        binding.saveButton.setOnClickListener {
            viewModel.saveProfile(
                binding.usernameInput.text.toString(),
                binding.displayNameInput.text.toString(),
                binding.bioInput.text.toString(),
                binding.privateSwitch.isChecked
            )
        }

        observeData()
        viewModel.loadProfile()
    }

    private fun observeData() {
        viewModel.user.observe(this) { result ->
            if (result is Resource.Success) {
                val user = result.data ?: return@observe
                binding.usernameInput.setText(user.username)
                binding.displayNameInput.setText(user.displayName)
                binding.bioInput.setText(user.bio)
                binding.privateSwitch.isChecked = user.isPrivate
                binding.profileInitial.text = user.username.firstOrNull()?.uppercase() ?: "?"
                if (user.profileImageUrl.isNotEmpty()) {
                    binding.profileImage.visibility = View.VISIBLE
                    Glide.with(this).load(user.profileImageUrl).circleCrop().into(binding.profileImage)
                }
            }
        }
        viewModel.saveResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Profil güncellendi", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.photoUploadResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.saveButton.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                    Toast.makeText(this, "Fotoğraf yüklendi", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
