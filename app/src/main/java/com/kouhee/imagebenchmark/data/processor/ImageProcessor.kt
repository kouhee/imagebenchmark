package com.kouhee.imagebenchmark.data.processor

import com.kouhee.imagebenchmark.domain.model.ImageData

interface ImageProcessor {

    suspend fun process(image: ImageData): ImageData

}