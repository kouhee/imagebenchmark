package com.kouhee.imagebenchmark.data.processor

import android.os.Trace
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeSobelProcessor : ImageProcessor {

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default + CoroutineName("NativeSobel")) {
        Trace.beginSection("NativeSobel_Process")
        try {
            detectEdgesSobel(image.pixels, image.width, image.height)
            image
        } finally {
            Trace.endSection()
        }
    }

    private external fun detectEdgesSobel(imageData: IntArray, width: Int, height: Int): IntArray
}
