package com.example.instagram_clone.presentation.upload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.instagram_clone.databinding.ActivityUploadBinding
import com.example.instagram_clone.util.Resource
import com.google.android.material.snackbar.Snackbar

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private val viewModel: UploadViewModel by viewModels()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerLaunchers()
        setupClickListeners()
        observeUploadState()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.imageContainer.setOnClickListener {
            selectImage(it)
        }

        binding.shareButton.setOnClickListener {
            val comment = binding.uploadCommentText.text.toString().trim()
            viewModel.uploadPost(comment)
        }
    }

    private fun selectImage(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openGallery()
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                Snackbar.make(view, "Galeri için izin gerekli", Snackbar.LENGTH_INDEFINITE)
                    .setAction("İzin Ver") {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    .setActionTextColor(
                        ContextCompat.getColor(this, com.example.instagram_clone.R.color.instagram_blue)
                    )
                    .show()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(intentToGallery)
    }

    private fun observeUploadState() {
        viewModel.uploadState.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    setLoadingState(true)
                }
                is Resource.Success -> {
                    setLoadingState(false)
                    Toast.makeText(this, "Gönderi paylaşıldı!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    setLoadingState(false)
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.uploadProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.shareButton.isEnabled = !isLoading
        binding.imageContainer.isEnabled = !isLoading

        if (isLoading) {
            binding.shareButton.alpha = 0.6f
            binding.shareButton.text = "Yükleniyor..."
        } else {
            binding.shareButton.alpha = 1.0f
            binding.shareButton.text = getString(com.example.instagram_clone.R.string.share)
        }
    }

    private fun registerLaunchers() {
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
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

                        viewModel.setImage(bitmap, uri)

                        binding.uploadImageView.setImageBitmap(bitmap)
                        binding.uploadImageView.visibility = View.VISIBLE
                        binding.placeholderContainer.visibility = View.GONE
                        binding.imageContainer.background = null

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                Toast.makeText(this, "Galeri izni gerekli!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
