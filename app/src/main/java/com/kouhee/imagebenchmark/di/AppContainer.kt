package com.kouhee.imagebenchmark.di

import com.kouhee.imagebenchmark.data.processor.ImageProcessorFactory
import com.kouhee.imagebenchmark.data.repository.ImageProcessorRepositoryImpl
import com.kouhee.imagebenchmark.data.repository.ImageProcessorRepositoryImplSimple
import com.kouhee.imagebenchmark.domain.repository.ImageProcessorRepository
import com.kouhee.imagebenchmark.domain.usecase.ProcessImageUseCase

object AppContainer {

    private val factory = ImageProcessorFactory()

    // 切り替えフラグ: true = ウォームアップ付き, false = シンプル版
    var useWarmup = true

    private val repositoryWithWarmup: ImageProcessorRepository =
        ImageProcessorRepositoryImpl(factory)

    private val repositorySimple: ImageProcessorRepository =
        ImageProcessorRepositoryImplSimple(factory)

    private val repository: ImageProcessorRepository
        get() = if (useWarmup) repositoryWithWarmup else repositorySimple

    val processImageUseCase: ProcessImageUseCase
        get() = ProcessImageUseCase(repository)

}
