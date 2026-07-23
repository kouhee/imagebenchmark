package com.kouhee.imagebenchmark.data.processor

import android.os.Trace
import android.util.Log
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeKotlinNaiveGrayScaleProcessor : ImageProcessor {

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    private external fun convertToGrayScale(imageData: IntArray, width: Int, height: Int): IntArray

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default + CoroutineName("NativeGrayScale")) {
        Log.d("NativeKotlinNaiveGrayScaleProcessor", "process START on ${Thread.currentThread().name}")
        Trace.beginSection("NativeGrayScale_Process")
        
        try {
            val pixels = image.pixels
            val convertedImageData = convertToGrayScale(pixels, image.width, image.height)

            Log.d("NativeKotlinNaiveGrayScaleProcessor", "process END on ${Thread.currentThread().name}")
            ImageData(image.width, image.height, convertedImageData)
        } finally {
            Trace.endSection()
        }
    }
}
