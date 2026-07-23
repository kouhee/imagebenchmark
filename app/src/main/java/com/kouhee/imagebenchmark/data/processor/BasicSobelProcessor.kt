package com.kouhee.imagebenchmark.data.processor

import android.os.Trace
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BasicSobelProcessor : ImageProcessor {

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default + CoroutineName("BasicSobel")) {
        Trace.beginSection("BasicSobel_Process")
        try {
            val width = image.width
            val height = image.height
            val source = image.pixels.copyOf()
            val output = image.pixels

            for (y in 0 until height) {
                val row = y * width
                if (y == 0 || y == height - 1) {
                    for (x in 0 until width) {
                        output[row + x] = 0xFF000000.toInt()
                    }
                    continue
                }

                output[row] = 0xFF000000.toInt()
                output[row + width - 1] = 0xFF000000.toInt()

                for (x in 1 until width - 1) {
                    val p00 = gray(source[(y - 1) * width + (x - 1)])
                    val p01 = gray(source[(y - 1) * width + x])
                    val p02 = gray(source[(y - 1) * width + (x + 1)])

                    val p10 = gray(source[y * width + (x - 1)])
                    val p12 = gray(source[y * width + (x + 1)])

                    val p20 = gray(source[(y + 1) * width + (x - 1)])
                    val p21 = gray(source[(y + 1) * width + x])
                    val p22 = gray(source[(y + 1) * width + (x + 1)])

                    val gx = (-p00 + p02) + (-2 * p10 + 2 * p12) + (-p20 + p22)
                    val gy = (p00 + 2 * p01 + p02) - (p20 + 2 * p21 + p22)

                    val magnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt().coerceIn(0, 255)
                    output[row + x] =
                        0xFF000000.toInt() or (magnitude shl 16) or (magnitude shl 8) or magnitude
                }
            }

            image
        } finally {
            Trace.endSection()
        }
    }

    private fun gray(pixel: Int): Int {
        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        return (r + g + b) / 3
    }
}
