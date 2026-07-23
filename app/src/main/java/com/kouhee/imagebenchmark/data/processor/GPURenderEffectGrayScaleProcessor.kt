package com.kouhee.imagebenchmark.data.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.os.Build
import android.os.Trace
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GPURenderEffectGrayScaleProcessor : ImageProcessor {

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default + CoroutineName("GPURenderEffectGrayScale")) {
        Trace.beginSection("GPURenderEffectGrayScale_Process")
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return@withContext image
            }

            val inputBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            inputBitmap.setPixels(image.pixels, 0, image.width, 0, 0, image.width, image.height)

            val outputBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)

            val paint = Paint()
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            val filter = ColorMatrixColorFilter(matrix)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val effect = RenderEffect.createColorFilterEffect(filter)
                try {
                    val setRenderEffectMethod = paint.javaClass.getMethod("setRenderEffect", RenderEffect::class.java)
                    setRenderEffectMethod.invoke(paint, effect)
                } catch (e: Exception) {
                    // Fallback to colorFilter if RenderEffect fails
                    paint.colorFilter = filter
                }
            } else {
                paint.colorFilter = filter
            }

            canvas.drawBitmap(inputBitmap, 0f, 0f, paint)

            outputBitmap.getPixels(image.pixels, 0, image.width, 0, 0, image.width, image.height)
            
            inputBitmap.recycle()
            outputBitmap.recycle()

            image
        } finally {
            Trace.endSection()
        }
    }
}
