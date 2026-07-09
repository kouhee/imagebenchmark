package com.kouhee.imagebenchmark.domain.usecase

import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ImageData
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.domain.repository.ImageProcessorRepository


class ProcessImageUseCase(
    private val repository: ImageProcessorRepository
) {

    suspend operator fun invoke(
        image: ImageData,
        filter: FilterType,
        engine: ProcessingEngine
    ): ImageData {

        return repository.process(
            image,
            filter,
            engine
        )
    }
}