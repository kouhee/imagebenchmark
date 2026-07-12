package com.kouhee.imagebenchmark.data.processor

import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeSobelProcessor : ImageProcessor {

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default) {
        detectEdgesSobel(image.pixels, image.width, image.height)
        image
    }

    private external fun detectEdgesSobel(imageData: IntArray, width: Int, height: Int): IntArray
}
