package com.kouhee.imagebenchmark.data.processor

import android.os.Trace
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BasicGrayScaleProcessor : ImageProcessor {

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default + CoroutineName("BasicGrayScale")) {
        Trace.beginSection("BasicGrayScale_Process")
        try {
            val pixels = image.pixels
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF
                val gray = (r + g + b) / 3
                pixels[i] = 0xFF000000.toInt() or (gray shl 16) or (gray shl 8) or gray
            }
            image
        } finally {
            Trace.endSection()
        }
    }
}
