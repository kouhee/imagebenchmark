package com.kouhee.imagebenchmark.data.processor

import com.kouhee.imagebenchmark.domain.model.ImageData

class NativeKotlinNaiveGrayScaleProcessor : ImageProcessor {

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    private external fun convertToGrayScale(imageData: IntArray, width: Int, height: Int): IntArray

    override fun process(image: ImageData): ImageData {

        val pixels = image.pixels

        // convertToGrayScaleの中に処理を書く
        val convertedImageData = convertToGrayScale(pixels, image.width, image.height)

        return ImageData(image.width, image.height, convertedImageData)

    }
}