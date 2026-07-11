package com.kouhee.imagebenchmark.di

import com.kouhee.imagebenchmark.data.processor.ImageProcessorFactory
import com.kouhee.imagebenchmark.data.repository.ImageProcessorRepositoryImpl
import com.kouhee.imagebenchmark.domain.usecase.ProcessImageUseCase

object AppContainer {

    private val factory = ImageProcessorFactory()

    private val repository =
        ImageProcessorRepositoryImpl(factory)

    val processImageUseCase =
        ProcessImageUseCase(repository)

}
