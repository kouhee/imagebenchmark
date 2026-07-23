package com.kouhee.imagebenchmark.domain.model

enum class FilterType {
    NONE,
    GRAYSCALE,
    SOBEL;

    fun displayName(): String {
        return when (this) {
            FilterType.NONE -> "None"
            FilterType.GRAYSCALE -> "GrayScale"
            FilterType.SOBEL -> "Sobel"
        }
    }

    fun supportedEngines(): Set<ProcessingEngine> {
        return when (this) {
            FilterType.NONE -> ProcessingEngine.entries.toSet()
            FilterType.GRAYSCALE -> ProcessingEngine.entries.toSet()
            FilterType.SOBEL -> setOf(
                ProcessingEngine.KOTLIN_BASIC,
                ProcessingEngine.NATIVE_BASIC,
                ProcessingEngine.GPU_RENDER_EFFECT
            )
        }
    }
}