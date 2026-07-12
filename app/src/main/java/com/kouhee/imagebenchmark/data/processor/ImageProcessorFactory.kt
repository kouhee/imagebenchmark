package com.kouhee.imagebenchmark.data.processor

import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine

class ImageProcessorFactory {
    
    // 生成したインスタンスをキャッシュするためのMap
    // デザインパターンの「Flyweightパターン」に近い考え方で、同一のプロセッサを再利用します
    private val processorCache = mutableMapOf<String, ImageProcessor>()

    fun create(filter: FilterType, engine: ProcessingEngine): ImageProcessor {
        val key = "${filter.name}_${engine.name}"

        return processorCache.getOrPut(key) {
            when (filter) {
                FilterType.GRAYSCALE -> {
                    when (engine) {
                        ProcessingEngine.BASIC -> BasicGrayScaleProcessor()
                        ProcessingEngine.KOTLIN_NAIVE -> KotlinNaiveGrayScaleProcessor()
                        ProcessingEngine.NATIVE -> NativeKotlinNaiveGrayScaleProcessor()
                        ProcessingEngine.INTERPOLATED -> InterpolatedGrayScaleProcessor()
                        ProcessingEngine.NATIVE_INTERPOLATED -> NativeInterpolatedGrayScaleProcessor()
                    }
                }
                FilterType.SOBEL -> {
                    BasicGrayScaleProcessor()
                }
            }
        }
    }
}
