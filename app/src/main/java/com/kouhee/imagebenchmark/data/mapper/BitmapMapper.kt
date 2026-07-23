package com.kouhee.imagebenchmark.data.mapper

import android.graphics.Bitmap
import com.kouhee.imagebenchmark.domain.model.ImageData
import androidx.core.graphics.createBitmap

object BitmapMapper {

    fun toImageData(bitmap: Bitmap): ImageData {

        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)

        bitmap.getPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        return ImageData(
            width,
            height,
            pixels
        )
    }

    fun toBitmap(image: ImageData, targetBitmap: Bitmap? = null): Bitmap {
        val bitmap = if (targetBitmap != null && 
            targetBitmap.width == image.width && 
            targetBitmap.height == image.height &&
            targetBitmap.isMutable) {
            targetBitmap
        } else {
            createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        }

        bitmap.setPixels(
            image.pixels,
            0,
            image.width,
            0,
            0,
            image.width,
            image.height
        )

        return bitmap
    }
}