package com.kouhee.imagebenchmark.domain.model

enum class ProcessingEngine {
    KOTLIN_BASIC,
    KOTLIN_UPDATE,
    NATIVE_BASIC,
    KOTLIN_INTERPOLATED,
    NATIVE_INTERPOLATED;

    fun displayName(): String {
        return when (this) {
            ProcessingEngine.KOTLIN_BASIC -> "Kotlin Basic"
            ProcessingEngine.KOTLIN_UPDATE -> "Kotlin Update"
            ProcessingEngine.NATIVE_BASIC -> "Native Basic"
            ProcessingEngine.KOTLIN_INTERPOLATED -> "Kotlin Interpolated"
            ProcessingEngine.NATIVE_INTERPOLATED -> "Native Interpolated"
        }
    }
}