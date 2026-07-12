package com.kouhee.imagebenchmark.data.processor

import android.util.Log
import com.kouhee.imagebenchmark.domain.model.ImageData

class NativeKotlinNaiveGrayScaleProcessor : ImageProcessor {

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    private external fun convertToGrayScale(imageData: IntArray, width: Int, height: Int): IntArray

    override suspend fun process(image: ImageData): ImageData {
        Log.d("NativeKotlinNaiveGrayScaleProcessor", "process START on ${Thread.currentThread().name}")
        val traceId = System.nanoTime().toInt()
        android.os.Trace.beginAsyncSection("NativeGrayScale", traceId)

        val pixels = image.pixels
        val convertedImageData = convertToGrayScale(pixels, image.width, image.height)

        Log.d("NativeKotlinNaiveGrayScaleProcessor", "process END on ${Thread.currentThread().name}")
        android.os.Trace.endAsyncSection("NativeGrayScale", traceId)
        return ImageData(image.width, image.height, convertedImageData)

    }
}