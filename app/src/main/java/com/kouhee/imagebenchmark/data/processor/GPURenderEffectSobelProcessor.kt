package com.kouhee.imagebenchmark.data.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Trace
import com.kouhee.imagebenchmark.domain.model.ImageData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GPURenderEffectSobelProcessor : ImageProcessor {

    private val SOBEL_SKSL = """
        uniform shader inputTexture;

        float gray(half4 color) {
            return (color.r + color.g + color.b) / 3.0;
        }

        half4 main(float2 fragCoord) {
            float p00 = gray(inputTexture.eval(fragCoord + float2(-1, -1)));
            float p01 = gray(inputTexture.eval(fragCoord + float2( 0, -1)));
            float p02 = gray(inputTexture.eval(fragCoord + float2( 1, -1)));
            float p10 = gray(inputTexture.eval(fragCoord + float2(-1,  0)));
            float p12 = gray(inputTexture.eval(fragCoord + float2( 1,  0)));
            float p20 = gray(inputTexture.eval(fragCoord + float2(-1,  1)));
            float p21 = gray(inputTexture.eval(fragCoord + float2( 0,  1)));
            float p22 = gray(inputTexture.eval(fragCoord + float2( 1,  1)));

            float gx = (-1.0 * p00) + (1.0 * p02) + (-2.0 * p10) + (2.0 * p12) + (-1.0 * p20) + (1.0 * p22);
            float gy = (1.0 * p00) + (2.0 * p01) + (1.0 * p02) + (-1.0 * p20) + (-2.0 * p21) + (-1.0 * p22);

            float magnitude = sqrt(gx * gx + gy * gy);
            return half4(magnitude, magnitude, magnitude, 1.0);
        }
    """.trimIndent()

    override suspend fun process(image: ImageData): ImageData = withContext(Dispatchers.Default + CoroutineName("GPURenderEffectSobel")) {
        Trace.beginSection("GPURenderEffectSobel_Process")
        try {
            // RuntimeShader requires API 33
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@withContext image
            }

            val inputBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            inputBitmap.setPixels(image.pixels, 0, image.width, 0, 0, image.width, image.height)

            val outputBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val shader = RuntimeShader(SOBEL_SKSL)

                val paint = Paint()
                val effect = RenderEffect.createRuntimeShaderEffect(shader, "inputTexture")
                // Attempt to set render effect
                try {
                    val setRenderEffectMethod = paint.javaClass.getMethod("setRenderEffect", RenderEffect::class.java)
                    setRenderEffectMethod.invoke(paint, effect)
                } catch (e: Exception) {
                    // Fallback or handle error
                }

                canvas.drawBitmap(inputBitmap, 0f, 0f, paint)
            }

            outputBitmap.getPixels(image.pixels, 0, image.width, 0, 0, image.width, image.height)

            inputBitmap.recycle()
            outputBitmap.recycle()

            image
        } finally {
            Trace.endSection()
        }
    }
}
