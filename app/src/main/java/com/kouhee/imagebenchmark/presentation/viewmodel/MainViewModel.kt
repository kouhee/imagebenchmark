package com.kouhee.imagebenchmark.presentation.viewmodel

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Trace
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kouhee.imagebenchmark.data.mapper.BitmapMapper
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ImageData
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.domain.usecase.ProcessImageUseCase
import com.kouhee.imagebenchmark.presentation.state.EngineTimingStats
import com.kouhee.imagebenchmark.presentation.state.MainUiState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var originalImageData: ImageData? = null
    private var processingBuffer: IntArray? = null

    fun setJniString(text: String) {
        _uiState.value = _uiState.value.copy(jniString = text)
    }

    fun setFilter(filter: FilterType) {
        val supportedEngines = filter.supportedEngines()
        val currentEngine = _uiState.value.selectedEngine
        
        val newEngine = if (currentEngine in supportedEngines) {
            currentEngine
        } else {
            supportedEngines.first()
        }

        val stats = _uiState.value.engineTimingStats[newEngine]

        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            selectedEngine = newEngine,
            fastestTimeUs = stats?.fastestTimeUs,
            slowestTimeUs = stats?.slowestTimeUs,
            elapsedTimeUs = 0.0
        )
    }

    fun setEngine(engine: ProcessingEngine) {
        val stats = _uiState.value.engineTimingStats[engine]
        _uiState.value = _uiState.value.copy(
            selectedEngine = engine,
            fastestTimeUs = stats?.fastestTimeUs,
            slowestTimeUs = stats?.slowestTimeUs,
            elapsedTimeUs = 0.0
        )
    }

    fun setBitmap(bitmap: Bitmap, width: Int, height: Int, fileSize: Long) {
        val imageData = BitmapMapper.toImageData(bitmap)
        originalImageData = imageData
        _uiState.value = _uiState.value.copy(
            inputBitmap = bitmap,
            outputBitmap = null,
            originalWidth = width,
            originalHeight = height,
            originalFileSize = fileSize,
            elapsedTimeUs = 0.0,
            fastestTimeUs = null,
            slowestTimeUs = null,
            engineTimingStats = emptyMap(),
            timeHistoryUs = emptyList()
        )
    }

    fun loadImage(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return@launch
                
                // Get original dimensions
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(contentResolver.openInputStream(uri), null, options)
                
                // Get file size
                var fileSize = 0L
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                    fileSize = fd.length
                }

                // Load bitmap
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                withContext(Dispatchers.Main) {
                    setBitmap(bitmap, options.outWidth, options.outHeight, fileSize)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun processImage() {
        val image = originalImageData ?: return

        val context = Dispatchers.Default + CoroutineName("MainImageProcessingTask")
        viewModelScope.launch(context) {
            val traceId = System.nanoTime().toInt()
            Trace.beginAsyncSection("MainImageProcessingTask", traceId)
            try {
                val state = _uiState.value
                val buffer = if (processingBuffer == null || processingBuffer!!.size != image.pixels.size) {
                    image.pixels.copyOf().also { processingBuffer = it }
                } else {
                    processingBuffer!!.also {
                        System.arraycopy(image.pixels, 0, it, 0, image.pixels.size)
                    }
                }
                val imageToProcess = ImageData(image.width, image.height, buffer)

                val result = processImageUseCase(
                    imageToProcess,
                    state.selectedFilter,
                    state.selectedEngine
                )

                val bitmap = BitmapMapper.toBitmap(result)

                withContext(Dispatchers.Main) {
                    val currentState = _uiState.value
                    val selectedEngine = state.selectedEngine
                    val existingStats = currentState.engineTimingStats[selectedEngine]
                    val updatedStats = EngineTimingStats(
                        fastestTimeUs = existingStats?.fastestTimeUs?.let { minOf(it, result.processingTimeUs) }
                            ?: result.processingTimeUs,
                        slowestTimeUs = existingStats?.slowestTimeUs?.let { maxOf(it, result.processingTimeUs) }
                            ?: result.processingTimeUs
                    )
                    val updatedStatsMap = currentState.engineTimingStats + (selectedEngine to updatedStats)
                    
                    val newHistory = (listOf(result.processingTimeUs) + currentState.timeHistoryUs).take(10)

                    _uiState.value = _uiState.value.copy(
                        outputBitmap = bitmap,
                        elapsedTimeUs = result.processingTimeUs,
                        fastestTimeUs = updatedStats.fastestTimeUs,
                        slowestTimeUs = updatedStats.slowestTimeUs,
                        engineTimingStats = updatedStatsMap,
                        timeHistoryUs = newHistory
                    )
                }
            } finally {
                Trace.endAsyncSection("MainImageProcessingTask", traceId)
            }
        }
    }
}
