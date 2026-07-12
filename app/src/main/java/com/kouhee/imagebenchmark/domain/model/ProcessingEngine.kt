package com.kouhee.imagebenchmark.domain.model

enum class ProcessingEngine {
    BASIC,
    KOTLIN_NAIVE,
    NATIVE,
    INTERPOLATED,
    NATIVE_INTERPOLATED;

    fun displayName(): String {
        return when (this) {
            ProcessingEngine.BASIC -> "Kotlin Base"
            ProcessingEngine.KOTLIN_NAIVE -> "Kotlin Update"
            ProcessingEngine.NATIVE -> "Native"
            ProcessingEngine.INTERPOLATED -> "Interpolated"
            ProcessingEngine.NATIVE_INTERPOLATED -> "Native Interpolated"
        }
    }
}