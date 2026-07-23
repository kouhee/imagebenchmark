package com.kouhee.imagebenchmark.data.processor

import android.os.Trace
import android.util.Log
import com.kouhee.imagebenchmark.common.timing.ProcessingTimer
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeInterpolatedGrayScaleProcessor : ImageProcessor {

    init {
        System.loadLibrary("native-lib")
    }

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default + CoroutineName("NativeInterpolatedGrayScale")) {
        Log.d("NativeInterpolated", "process START on ${Thread.currentThread().name}")
        Trace.beginSection("NativeInterpolatedGrayScale_Process")
        
        try {
            val start = ProcessingTimer.mark()

            convertToGrayScale(image.pixels, image.width, image.height)

            val timing = ProcessingTimer.durationUs(start, ProcessingTimer.mark())
            Log.d("NativeInterpolated", "process END on ${Thread.currentThread().name}")
            Log.i("NativeInterpolated", "Total processing time: ${timing.timeMs} ms")
            image
        } finally {
            Trace.endSection()
        }
    }

    external fun convertToGrayScale(pixels: IntArray, width: Int, height: Int): IntArray
}
