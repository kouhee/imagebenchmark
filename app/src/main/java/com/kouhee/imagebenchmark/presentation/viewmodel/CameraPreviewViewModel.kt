package com.kouhee.imagebenchmark.presentation.viewmodel

import android.graphics.Bitmap
import android.os.Trace
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kouhee.imagebenchmark.data.mapper.BitmapMapper
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.domain.usecase.ProcessImageUseCase
import com.kouhee.imagebenchmark.presentation.state.CameraPreviewUiState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

class CameraPreviewViewModel(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraPreviewUiState())
    val uiState = _uiState.asStateFlow()

    private val frameTimestamps = mutableListOf<Long>()
    private val MAX_FRAME_SAMPLES = 20
    private var processingJob: Job? = null
    
    private var isCleared = false
    
    // Reuse pixel array and bitmap to reduce GC pressure
    private var cachedPixels: IntArray? = null
    private var cachedBitmap: Bitmap? = null

    fun processImageProxy(
        imageProxy: ImageProxy,
        selectedFilter: FilterType,
        selectedEngine: ProcessingEngine
    ) {
        // Skip frame if previous one is still being processed or if cleared
        if (isCleared || processingJob?.isActive == true) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = imageProxy.width
        val height = imageProxy.height

        // Add CoroutineName to track the processing coroutine
        val context = Dispatchers.Default + CoroutineName("ImageProcessingTask")
        processingJob = (viewModelScope + context).launch {
            val traceId = System.nanoTime().toInt()
            try {
                Trace.beginAsyncSection("ProcessFrame", traceId)
                // 1. Get Bitmap from ImageProxy (reusing bitmap if possible)
                // Note: imageProxy.toBitmap() creates a new bitmap, 
                // but we can reuse our cachedBitmap for the processed result later.
                val sourceBitmap = imageProxy.toBitmap()
                imageProxy.close()

                var finalBitmap: Bitmap = sourceBitmap
                var timeMs = 0.0

                // 2. Only process if filter is not NONE
                if (selectedFilter != FilterType.NONE) {
                    val pixelCount = width * height
                    if (cachedPixels == null || cachedPixels!!.size != pixelCount) {
                        cachedPixels = IntArray(pixelCount)
                    }
                    
                    // Copy to array for processing
                    sourceBitmap.getPixels(cachedPixels!!, 0, width, 0, 0, width, height)
                    
                    val startTime = System.nanoTime()
                    val resultData = processImageUseCase(
                        image = com.kouhee.imagebenchmark.domain.model.ImageData(width, height, cachedPixels!!),
                        filter = selectedFilter,
                        engine = selectedEngine
                    )
                    val endTime = System.nanoTime()
                    timeMs = (endTime - startTime) / 1_000_000.0
                    
                    // Update bitmap with processed pixels (REUSING cachedBitmap)
                    cachedBitmap = BitmapMapper.toBitmap(resultData, cachedBitmap)
                    finalBitmap = cachedBitmap!!
                }

                if (!isCleared) {
                    withContext(Dispatchers.Main) {
                        updateUiState(timeMs, finalBitmap, rotation)
                    }
                }
            } catch (e: Exception) {
                imageProxy.close()
            } finally {
                Trace.endAsyncSection("ProcessFrame", traceId)
            }
        }
    }

    private fun updateUiState(timeMs: Double, bitmap: Bitmap, rotation: Int) {
        val currentTime = System.nanoTime()
        frameTimestamps.add(currentTime)
        if (frameTimestamps.size > MAX_FRAME_SAMPLES) frameTimestamps.removeAt(0)

        val currentFps = if (frameTimestamps.size > 1) {
            val durationSec = (frameTimestamps.last() - frameTimestamps.first()) / 1_000_000_000.0
            (frameTimestamps.size - 1) / durationSec
        } else 0.0

        _uiState.value = _uiState.value.copy(
            processedBitmap = bitmap,
            processingTimeMs = timeMs,
            fps = currentFps,
            rotationDegrees = rotation
        )
    }

    fun clearResources() {
        isCleared = true
        processingJob?.cancel()
        nativeShutdownThreadPool()
        cachedPixels = null
        cachedBitmap = null
        frameTimestamps.clear()
        // Do not clear the bitmap immediately, as it might cause a flicker or empty screen during transition
        // _uiState.value = CameraPreviewUiState() 
    }

    fun resetClearedState() {
        isCleared = false
    }

    override fun onCleared() {
        clearResources()
    }

    private external fun nativeShutdownThreadPool()
}
