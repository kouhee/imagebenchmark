package com.kouhee.imagebenchmark.domain.model

data class ImageData(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
    val processingTimeUs: Double = 0.0
)
