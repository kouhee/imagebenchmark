package com.kouhee.imagebenchmark.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kouhee.imagebenchmark.data.mapper.BitmapMapper
import com.kouhee.imagebenchmark.domain.model.ImageData
import com.kouhee.imagebenchmark.domain.usecase.ProcessImageUseCase
import com.kouhee.imagebenchmark.presentation.state.MainUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var currentImageData: ImageData? = null

    fun setJniString(text: String) {
        _uiState.value = _uiState.value.copy(jniString = text)
    }

    fun setBitmap(bitmap: Bitmap, originalWidth: Int, originalHeight: Int, fileSize: Long) {

        currentImageData = BitmapMapper.toImageData(bitmap)

        _uiState.value = _uiState.value.copy(
            inputBitmap = bitmap,
            outputBitmap = bitmap,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            originalFileSize = fileSize
        )
    }

    fun processImage() {
        val image = currentImageData ?: return

        viewModelScope.launch {

            val start = System.nanoTime()

            val result = processImageUseCase(
                image,
                _uiState.value.selectedFilter,
                _uiState.value.selectedEngine
            )

            val end = System.nanoTime()

            val bitmap = BitmapMapper.toBitmap(result)

            _uiState.value = _uiState.value.copy(
                outputBitmap = bitmap,
                elapsedTime = (end - start) / 1_000_000.0
            )

            currentImageData = result
        }
    }
}