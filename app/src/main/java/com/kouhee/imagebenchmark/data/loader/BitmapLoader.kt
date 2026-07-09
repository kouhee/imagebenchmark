package com.kouhee.imagebenchmark.data.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object BitmapLoader {

    fun loadDemoImage(context: Context): Bitmap {
        return context.assets.open("roadster1.jpg").use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inScaled = false // Ensure native resolution
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(inputStream, null, options)!!
        }
    }
}