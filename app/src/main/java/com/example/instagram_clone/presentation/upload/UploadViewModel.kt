package com.example.instagram_clone.presentation.upload

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram_clone.data.repository.PostRepositoryImpl
import com.example.instagram_clone.domain.repository.PostRepository
import com.example.instagram_clone.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class UploadViewModel(
    private val postRepository: PostRepository = PostRepositoryImpl()
) : ViewModel() {

    private val _uploadState = MutableLiveData<Resource<Unit>>()
    val uploadState: LiveData<Resource<Unit>> = _uploadState

    var selectedBitmap: Bitmap? = null
    var selectedUri: Uri? = null

    fun uploadPost(comment: String) {
        val bitmap = selectedBitmap
        if (bitmap == null) {
            _uploadState.value = Resource.Error("Lütfen bir fotoğraf seçin!")
            return
        }
        if (comment.isBlank()) {
            _uploadState.value = Resource.Error("Lütfen bir yorum yazın!")
            return
        }
        if (comment.length > 2200) {
            _uploadState.value = Resource.Error("Açıklama karakter sınırını aştı (maks 2200)!")
            return
        }

        viewModelScope.launch {
            _uploadState.value = Resource.Loading()
            val imageBytes = withContext(Dispatchers.IO) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                baos.toByteArray()
            }
            postRepository.uploadPost(imageBytes, comment).collectLatest { result ->
                _uploadState.postValue(result)
            }
        }
    }

    fun setImage(bitmap: Bitmap, uri: Uri) {
        selectedBitmap = bitmap
        selectedUri = uri
    }

    fun hasImage(): Boolean = selectedBitmap != null
}
