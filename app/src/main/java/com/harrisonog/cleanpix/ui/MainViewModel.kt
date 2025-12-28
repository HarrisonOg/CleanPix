package com.harrisonog.cleanpix.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.cleanpix.R
import com.harrisonog.cleanpix.data.MetadataStripper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageState(
    val originalUri: Uri? = null,
    val cleanedUri: Uri? = null,
    val originalMetadata: Map<String, String> = emptyMap(),
    val cleanedMetadata: Map<String, String> = emptyMap(),
    val isProcessing: Boolean = false,
    val error: String? = null,
    val savedPath: String? = null
)

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(ImageState())
    val state: StateFlow<ImageState> = _state.asStateFlow()

    private lateinit var metadataStripper: MetadataStripper
    private lateinit var context: Context
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            this.context = context
            metadataStripper = MetadataStripper(context)
            isInitialized = true
        }
    }

    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }

            try {
                // Read original metadata
                val metadata = metadataStripper.readMetadata(uri)

                _state.update {
                    it.copy(
                        originalUri = uri,
                        originalMetadata = metadata,
                        cleanedUri = null,
                        cleanedMetadata = emptyMap(),
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        error = context.getString(R.string.error_read_image, e.message ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun stripMetadata() {
        viewModelScope.launch {
            val originalUri = _state.value.originalUri ?: return@launch

            _state.update { it.copy(isProcessing = true, error = null) }

            metadataStripper.stripMetadata(originalUri)
                .onSuccess { cleanedUri ->
                    // Verify cleaned image has no metadata
                    val cleanedMetadata = metadataStripper.readMetadata(cleanedUri)

                    _state.update {
                        it.copy(
                            cleanedUri = cleanedUri,
                            cleanedMetadata = cleanedMetadata,
                            isProcessing = false
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = context.getString(R.string.error_strip_metadata, exception.message ?: "Unknown error")
                        )
                    }
                }
        }
    }

    fun saveCleanedImage(customFileName: String) {
        viewModelScope.launch {
            val cleanedUri = _state.value.cleanedUri ?: return@launch

            _state.update { it.copy(isProcessing = true, error = null) }

            // Add .jpg extension if not present
            val fileName = if (customFileName.endsWith(".jpg", ignoreCase = true)) {
                customFileName
            } else {
                "$customFileName.jpg"
            }

            metadataStripper.saveToPermanentStorage(cleanedUri, fileName)
                .onSuccess { file ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            savedPath = file.absolutePath
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = context.getString(R.string.error_save_image, exception.message ?: "Unknown error")
                        )
                    }
                }
        }
    }

    fun clearState() {
        _state.value = ImageState()
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun dismissSavedMessage() {
        _state.update { it.copy(savedPath = null) }
    }
}