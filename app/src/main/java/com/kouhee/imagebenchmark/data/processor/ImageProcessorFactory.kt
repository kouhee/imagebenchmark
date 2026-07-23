package com.kouhee.imagebenchmark.data.processor

import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine

class ImageProcessorFactory {

    private val processorCache = mutableMapOf<String, ImageProcessor>()

    fun create(filter: FilterType, engine: ProcessingEngine): ImageProcessor {
        val key = "${filter.name}_${engine.name}"

        return processorCache.getOrPut(key) {
            when (filter) {
                FilterType.NONE -> IdentityProcessor()
                FilterType.GRAYSCALE -> createGrayScaleProcessor(engine)
                FilterType.SOBEL -> createSobelProcessor(engine)
            }
        }
    }

    private fun createGrayScaleProcessor(engine: ProcessingEngine): ImageProcessor {
        return when (engine) {
            ProcessingEngine.KOTLIN_BASIC -> BasicGrayScaleProcessor()
            ProcessingEngine.KOTLIN_UPDATE -> KotlinNaiveGrayScaleProcessor()
            ProcessingEngine.NATIVE_BASIC -> NativeKotlinNaiveGrayScaleProcessor()
            ProcessingEngine.KOTLIN_INTERPOLATED -> InterpolatedGrayScaleProcessor()
            ProcessingEngine.NATIVE_INTERPOLATED -> NativeInterpolatedGrayScaleProcessor()
            ProcessingEngine.GPU_RENDER_EFFECT -> GPURenderEffectGrayScaleProcessor()
        }
    }

    private fun createSobelProcessor(engine: ProcessingEngine): ImageProcessor {
        return when (engine) {
            ProcessingEngine.KOTLIN_BASIC -> BasicSobelProcessor()
            ProcessingEngine.NATIVE_BASIC -> NativeSobelProcessor()
            ProcessingEngine.GPU_RENDER_EFFECT -> GPURenderEffectSobelProcessor()
            else -> throw IllegalArgumentException("Unsupported engine for SOBEL: $engine")
        }
    }
}
