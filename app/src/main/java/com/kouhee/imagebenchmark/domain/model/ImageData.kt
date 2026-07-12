package com.kouhee.imagebenchmark.domain.model

data class ImageData(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
    var processingTimeUs: Double = 0.0,
    var processingTimeMs: Double = 0.0,
)
