package com.kouhee.imagebenchmark.domain.model

enum class FilterType {
    GRAYSCALE,
    SOBEL;

    fun displayName(): String {
        return when (this) {
            FilterType.GRAYSCALE -> "GrayScale"
            FilterType.SOBEL -> "Sobel"
        }
    }
}