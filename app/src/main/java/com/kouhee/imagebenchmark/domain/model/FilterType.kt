package com.kouhee.imagebenchmark.domain.model

enum class FilterType {
    GRAYSCALE,
    SOBEL;

    fun displayName(): String {
        return when (this) {
            FilterType.GRAYSCALE -> "GrayScale"
            FilterType.SOBEL -> "Sobel(EdgeDetection)"
        }
    }

    fun supportedEngines(): Set<ProcessingEngine> {
        return when (this) {
            FilterType.GRAYSCALE -> ProcessingEngine.entries.toSet()
            FilterType.SOBEL -> setOf(ProcessingEngine.KOTLIN_BASIC, ProcessingEngine.NATIVE_BASIC)
        }
    }
}