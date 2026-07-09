package com.kouhee.imagebenchmark.presentation.state

import android.graphics.Bitmap
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine

data class MainUiState(
    val inputBitmap: Bitmap? = null,
    val outputBitmap: Bitmap? = null,
    val selectedFilter: FilterType = FilterType.GRAYSCALE,
    val selectedEngine: ProcessingEngine = ProcessingEngine.KOTLIN_NAIVE,
    val elapsedTime: Double = 0.0,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalFileSize: Long = 0L
)