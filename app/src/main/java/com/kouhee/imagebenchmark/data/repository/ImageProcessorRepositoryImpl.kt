package com.kouhee.imagebenchmark.data.repository

import com.kouhee.imagebenchmark.data.processor.ImageProcessorFactory
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ImageData
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.domain.repository.ImageProcessorRepository

class ImageProcessorRepositoryImpl(
    private val factory: ImageProcessorFactory
) : ImageProcessorRepository {

    override suspend fun process(
        image: ImageData,
        filter: FilterType,
        engine: ProcessingEngine
    ): ImageData {
        val processor = factory.create(filter, engine)
        
        val start = System.nanoTime()
        val result = processor.process(image)
        val end = System.nanoTime()
        
        return result.copy(processingTimeUs = (end - start) / 1_000.0)
    }
}
