package com.kouhee.imagebenchmark.presentation.state

import android.graphics.Bitmap
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine

data class EngineTimingStats(
    val fastestTimeUs: Double,
    val slowestTimeUs: Double,
)

data class MainUiState(
    val inputBitmap: Bitmap? = null,
    val outputBitmap: Bitmap? = null,
    val selectedFilter: FilterType = FilterType.GRAYSCALE,
    val selectedEngine: ProcessingEngine = ProcessingEngine.NATIVE_BASIC,
    val elapsedTimeUs: Double = 0.0,
    val fastestTimeUs: Double? = null,
    val slowestTimeUs: Double? = null,
    val engineTimingStats: Map<ProcessingEngine, EngineTimingStats> = emptyMap(),
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalFileSize: Long = 0L,
    val jniString: String = "",
    val threadCount: Int = Runtime.getRuntime().availableProcessors(),
)