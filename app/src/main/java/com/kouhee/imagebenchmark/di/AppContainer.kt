package com.kouhee.imagebenchmark.di

import com.kouhee.imagebenchmark.data.processor.KotlinNaiveGrayScaleProcessor
import com.kouhee.imagebenchmark.data.repository.ImageProcessorRepositoryImpl
import com.kouhee.imagebenchmark.domain.usecase.ProcessImageUseCase

object AppContainer {

    private val processor =
        KotlinNaiveGrayScaleProcessor()

    private val repository =
        ImageProcessorRepositoryImpl(processor)

    val processImageUseCase =
        ProcessImageUseCase(repository)

}