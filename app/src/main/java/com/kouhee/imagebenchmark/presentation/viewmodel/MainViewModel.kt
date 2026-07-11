package com.kouhee.imagebenchmark.presentation.viewmodel

import android.graphics.Bitmap
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

        originalImageData = BitmapMapper.toImageData(bitmap)

        _uiState.value = _uiState.value.copy(
            inputBitmap = bitmap,
            outputBitmap = bitmap,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            originalFileSize = fileSize
        )
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