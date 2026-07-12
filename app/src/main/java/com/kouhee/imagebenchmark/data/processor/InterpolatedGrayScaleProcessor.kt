package com.kouhee.imagebenchmark.data.processor

import android.util.Log
import androidx.tracing.Trace
import com.kouhee.imagebenchmark.common.timing.ProcessingTimer
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class InterpolatedGrayScaleProcessor : ImageProcessor {

    private val grayTable = IntArray(65536)

    init {
        for (rgb565 in 0 until 65536) {
            val r = ((rgb565 shr 11) and 0x1F) * 255 / 31
            val g = ((rgb565 shr 5) and 0x3F) * 255 / 63
            val b = (rgb565 and 0x1F) * 255 / 31

            val gray = (r + g + b) / 3

            grayTable[rgb565] =
                0xFF000000.toInt() or
                        (gray shl 16) or
                        (gray shl 8) or
                        gray
        }
    }

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default) {
        Log.d("InterpolatedGrayScale", "process START")
        val traceId = System.nanoTime().toInt()
        android.os.Trace.beginAsyncSection("InterpolatedGrayScale", traceId)
        
        val start = ProcessingTimer.mark()

        val pixels = image.pixels
        val totalPixels = pixels.size
        val numThreads = Runtime.getRuntime().availableProcessors()

        Log.i("InterpolatedGrayScale", "=== INTERPOLATED PROCESSING (1/2 compute + linear interpolation) ===")
        
        coroutineScope {
            val chunkSize = totalPixels / numThreads

            val jobs = (0 until numThreads).map { i ->
                async {
                    val start = i * chunkSize
                    val end = if (i == numThreads - 1) totalPixels else (i + 1) * chunkSize
                    processInterpolated(pixels, start, end)
                }
            }
            jobs.awaitAll()
        }

        val timing = ProcessingTimer.durationUs(start, ProcessingTimer.mark())
        Log.i("InterpolatedGrayScale", "Interpolated processing time: ${timing.timeMs} ms")
        Trace.endAsyncSection("InterpolatedGrayScale", traceId)
        image
    }

    private fun processInterpolated(
        pixels: IntArray,
        start: Int,
        end: Int
    ) {
        val table = grayTable
        
        // Align start to even pixel
        val alignedStart = if (start % 2 == 0) start else start + 1
        
        var i = alignedStart
        
        // Process pairs: compute even, interpolate odd
        while (i + 1 < end) {
            // Even pixel (2i): Convert to grayscale
            val pixel0 = pixels[i]
            val rgb565_0 = ((pixel0 ushr 8) and 0xF800) or
                    ((pixel0 ushr 5) and 0x07E0) or
                    ((pixel0 ushr 3) and 0x001F)
            val gray0 = table[rgb565_0]
            pixels[i] = gray0

            // Odd pixel (2i+1): Interpolate
            if (i + 1 < end) {
                val pixel1 = pixels[i + 1]
                val rgb565_1 = ((pixel1 ushr 8) and 0xF800) or
                        ((pixel1 ushr 5) and 0x07E0) or
                        ((pixel1 ushr 3) and 0x001F)
                val gray1 = table[rgb565_1]
                
                // Linear interpolation: average of adjacent computed pixels
                val r0 = (gray0 shr 16) and 0xFF
                val g0 = (gray0 shr 8) and 0xFF
                val b0 = gray0 and 0xFF
                
                val r1 = (gray1 shr 16) and 0xFF
                val g1 = (gray1 shr 8) and 0xFF
                val b1 = gray1 and 0xFF
                
                val rInterp = (r0 + r1) / 2
                val gInterp = (g0 + g1) / 2
                val bInterp = (b0 + b1) / 2
                
                pixels[i + 1] = 0xFF000000.toInt() or (rInterp shl 16) or (gInterp shl 8) or bInterp
            }
            
            i += 2
        }
        
        // Handle remaining odd pixel at the end
        if (i < end) {
            val pixel = pixels[i]
            val rgb565 = ((pixel ushr 8) and 0xF800) or
                    ((pixel ushr 5) and 0x07E0) or
                    ((pixel ushr 3) and 0x001F)
            pixels[i] = table[rgb565]
        }
    }
}
