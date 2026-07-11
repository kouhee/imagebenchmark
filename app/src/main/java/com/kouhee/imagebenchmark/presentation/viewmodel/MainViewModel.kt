package com.kouhee.imagebenchmark.presentation.viewmodel

import android.graphics.Bitmap
import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kouhee.imagebenchmark.data.mapper.BitmapMapper
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ImageData
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.domain.usecase.ProcessImageUseCase
import com.kouhee.imagebenchmark.presentation.state.MainUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var originalImageData: ImageData? = null

    fun setJniString(text: String) {
        _uiState.value = _uiState.value.copy(jniString = text)
    }

    fun setFilter(filterType: FilterType) {
        _uiState.value = _uiState.value.copy(selectedFilter = filterType)
    }

    fun setEngine(engine: ProcessingEngine) {
        _uiState.value = _uiState.value.copy(selectedEngine = engine)
    }

    fun setBitmap(bitmap: Bitmap, originalWidth: Int, originalHeight: Int, fileSize: Long) {
        // UI update should be fast
        _uiState.value = _uiState.value.copy(
            inputBitmap = bitmap,
            outputBitmap = bitmap,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            originalFileSize = fileSize
        )

        // Heavy mapping in background
        viewModelScope.launch(Dispatchers.Default) {
            originalImageData = BitmapMapper.toImageData(bitmap)
        }
    }

    fun loadImage(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var originalWidth = 0
            var originalHeight = 0

            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)

                // Re-open stream for ExifInterface to handle rotation
                contentResolver.openInputStream(uri)?.use { exifInput ->
                    val exif = ExifInterface(exifInput)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )

                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                        orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        originalWidth = options.outHeight
                        originalHeight = options.outWidth
                    } else {
                        originalWidth = options.outWidth
                        originalHeight = options.outHeight
                    }
                }
            }

            val fileSize = contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                if (it.length >= 0) it.length else 0L
            } ?: 0L

            val source = ImageDecoder.createSource(contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }

            val imageData = withContext(Dispatchers.Default) {
                BitmapMapper.toImageData(bitmap)
            }

            withContext(Dispatchers.Main) {
                originalImageData = imageData
                _uiState.value = _uiState.value.copy(
                    inputBitmap = bitmap,
                    outputBitmap = bitmap,
                    originalWidth = originalWidth,
                    originalHeight = originalHeight,
                    originalFileSize = fileSize
                )
            }
        }
    }

    fun processImage() {
        val image = originalImageData ?: return

        viewModelScope.launch {

            val result = withContext(Dispatchers.Default) {
                processImageUseCase(
                    image,
                    _uiState.value.selectedFilter,
                    _uiState.value.selectedEngine
                )
            }

            val bitmap = withContext(Dispatchers.Default) {
                BitmapMapper.toBitmap(result)
            }

            _uiState.value = _uiState.value.copy(
                outputBitmap = bitmap,
                elapsedTimeUs = result.processingTimeUs
            )
        }
    }
}