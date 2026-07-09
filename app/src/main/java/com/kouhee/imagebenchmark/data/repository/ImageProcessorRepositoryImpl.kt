package com.kouhee.imagebenchmark.data.repository

import com.kouhee.imagebenchmark.data.processor.ImageProcessor
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ImageData
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.domain.repository.ImageProcessorRepository

class ImageProcessorRepositoryImpl(
    private val processor: ImageProcessor
) : ImageProcessorRepository {

    override suspend fun process(
        image: ImageData,
        filter: FilterType,
        engine: ProcessingEngine
    ): ImageData {

        return processor.process(image)

    }
}