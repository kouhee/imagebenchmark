package com.kouhee.imagebenchmark.presentation.state

import android.graphics.Bitmap

data class CameraPreviewUiState(
    val processedBitmap: Bitmap? = null,
    val processingTimeMs: Double = 0.0,
    val fps: Double = 0.0,
    val rotationDegrees: Int = 0
)
