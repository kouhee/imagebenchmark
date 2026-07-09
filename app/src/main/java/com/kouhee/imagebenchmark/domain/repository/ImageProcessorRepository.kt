package com.kouhee.imagebenchmark.domain.repository

import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ImageData
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine

interface ImageProcessorRepository {

    suspend fun process(
        image: ImageData,
        filter: FilterType,
        engine: ProcessingEngine
    ): ImageData
}