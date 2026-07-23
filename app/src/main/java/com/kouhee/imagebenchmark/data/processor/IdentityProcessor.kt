package com.kouhee.imagebenchmark.data.processor

import com.kouhee.imagebenchmark.domain.model.ImageData

class IdentityProcessor : ImageProcessor {
    override suspend fun process(image: ImageData): ImageData {
        return image
    }
}
