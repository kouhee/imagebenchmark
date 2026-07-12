package com.kouhee.imagebenchmark.data.processor

import android.util.Log
import androidx.tracing.Trace
import com.kouhee.imagebenchmark.common.timing.ProcessingTimer
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeInterpolatedGrayScaleProcessor : ImageProcessor {

    init {
        System.loadLibrary("native-lib")
    }

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default) {
        Log.d("NativeInterpolated", "process START on ${Thread.currentThread().name}")
        val traceId = System.nanoTime().toInt()
        Trace.beginAsyncSection("NativeInterpolatedGrayScale", traceId)
        
        val start = ProcessingTimer.mark()

        convertToGrayScale(image.pixels, image.width, image.height)

        val timing = ProcessingTimer.durationUs(start, ProcessingTimer.mark())
        Log.d("NativeInterpolated", "process END on ${Thread.currentThread().name}")
        Log.i("NativeInterpolated", "Total processing time: ${timing.timeMs} ms")
        Trace.endAsyncSection("NativeInterpolatedGrayScale", traceId)
        image
    }

    external fun convertToGrayScale(pixels: IntArray, width: Int, height: Int): IntArray
}
