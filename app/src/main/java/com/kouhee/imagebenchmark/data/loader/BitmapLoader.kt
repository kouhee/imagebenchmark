package com.kouhee.imagebenchmark.data.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kouhee.imagebenchmark.R

object BitmapLoader {

    fun loadDemoImage(context: Context): Bitmap {
        val options = BitmapFactory.Options().apply {
            inScaled = false // Disable scaling based on density
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeResource(
            context.resources,
            R.drawable.roadster1,
            options
        )
    }

}