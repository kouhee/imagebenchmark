package com.kouhee.imagebenchmark.domain.model

enum class ProcessingEngine {
    KOTLIN_NAIVE,
    KOTLIN_OPTIMIZED,
    NATIVE,
    NEON;

    fun displayName(): String {
        return when (this) {
            ProcessingEngine.KOTLIN_NAIVE -> "Naive Kotlin"
            ProcessingEngine.KOTLIN_OPTIMIZED -> "Optimized Kotlin"
            ProcessingEngine.NATIVE -> "Native"
            ProcessingEngine.NEON -> "NEON"
        }
    }
}