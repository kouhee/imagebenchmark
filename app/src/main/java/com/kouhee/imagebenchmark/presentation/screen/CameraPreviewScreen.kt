package com.kouhee.imagebenchmark.presentation.screen

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.presentation.util.getCameraProvider
import com.kouhee.imagebenchmark.presentation.viewmodel.CameraPreviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPreviewScreen(
    viewModel: CameraPreviewViewModel,
    selectedFilter: FilterType,
    selectedEngine: ProcessingEngine,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Reset ViewModel's cleared state when entering the screen
    LaunchedEffect(Unit) {
        viewModel.resetClearedState()
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
    }

    val cameraProviderResult = remember { context.getCameraProvider() }

    // Re-bind camera when filter or engine changes, or when lifecycle starts
    LaunchedEffect(selectedFilter, selectedEngine, lifecycleOwner) {
        val cameraProvider = cameraProviderResult.get()
        
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy: ImageProxy ->
            viewModel.processImageProxy(imageProxy, selectedFilter, selectedEngine)
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Ensure we are in a clean state before binding
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 1. Explicitly unbind the CameraX use case BEFORE clearing VM resources.
            // This ensures that the ImageAnalysis (and its ImageReader) is stopped
            // before we cancel processing jobs or shutdown the native thread pool.
            try {
                if (cameraProviderResult.isDone) {
                    val cameraProvider = cameraProviderResult.get()
                    cameraProvider.unbind(imageAnalysis)
                }
            } catch (e: Exception) {
                // Ignore if provider not available or already unbound
            }

            // 2. Clear ViewModel resources (cancels coroutines, clears arrays)
            viewModel.clearResources()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Camera (%.1f ms, %.1f fps)".format(uiState.processingTimeMs, uiState.fps))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            uiState.processedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Processed Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(uiState.rotationDegrees.toFloat())
                )
            }
        }
    }
}
