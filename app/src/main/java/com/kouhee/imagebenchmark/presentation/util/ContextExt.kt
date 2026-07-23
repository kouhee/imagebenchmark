package com.kouhee.imagebenchmark.presentation.util

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.common.util.concurrent.ListenableFuture

fun Context.getCameraProvider(): ListenableFuture<ProcessCameraProvider> {
    return ProcessCameraProvider.getInstance(this)
}
