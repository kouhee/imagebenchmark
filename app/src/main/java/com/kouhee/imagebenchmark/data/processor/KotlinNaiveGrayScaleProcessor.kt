package com.kouhee.imagebenchmark.data.processor

import com.kouhee.imagebenchmark.domain.model.ImageData

class KotlinNaiveGrayScaleProcessor : ImageProcessor {

    override fun process(image: ImageData): ImageData {

        val pixels = image.pixels

        for (i in pixels.indices) {

            val pixel = pixels[i]

            val a = pixel ushr 24
            val r = (pixel ushr 16) and 0xff
            val g = (pixel ushr 8) and 0xff
            val b = pixel and 0xff

            val gray = (r + g + b) / 3

            pixels[i] =
                (a shl 24) or
                        (gray shl 16) or
                        (gray shl 8) or
                        gray
        }

        return image

    }
}