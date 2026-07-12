package com.kouhee.imagebenchmark.data.processor

import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class KotlinNaiveGrayScaleProcessor : ImageProcessor {

    private val grayTable = IntArray(65536).apply {
        for (rgb565 in indices) {
            val r = ((rgb565 shr 11) and 0x1F) * 255 / 31
            val g = ((rgb565 shr 5) and 0x3F) * 255 / 63
            val b = (rgb565 and 0x1F) * 255 / 31
            val gray = (r + g + b) / 3
            this[rgb565] = 0xFF000000.toInt() or (gray shl 16) or (gray shl 8) or gray
        }
    }

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default) {
        val pixels = image.pixels
        val totalPixels = pixels.size
        val numThreads = Runtime.getRuntime().availableProcessors()
        val chunkSize = totalPixels / numThreads

        coroutineScope {
            val jobs = (0 until numThreads).map { i ->
                async {
                    val start = i * chunkSize
                    val end = if (i == numThreads - 1) totalPixels else (i + 1) * chunkSize
                    processRangeWithTable(pixels, start, end)
                }
            }
            jobs.awaitAll()
        }
        image
    }

    private fun processRangeWithTable(
        pixels: IntArray,
        start: Int,
        end: Int
    ) {
        val table = grayTable
        for (i in start until end) {
            val pixel = pixels[i]
            val rgb565 = ((pixel ushr 8) and 0xF800) or
                ((pixel ushr 5) and 0x07E0) or
                ((pixel ushr 3) and 0x001F)
            pixels[i] = table[rgb565]
        }
    }
}
