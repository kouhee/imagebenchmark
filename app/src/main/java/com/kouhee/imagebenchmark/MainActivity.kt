package com.kouhee.imagebenchmark

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import com.kouhee.imagebenchmark.data.loader.BitmapLoader
import com.kouhee.imagebenchmark.di.AppContainer
import com.kouhee.imagebenchmark.presentation.screen.MainScreen
import com.kouhee.imagebenchmark.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel = MainViewModel(
        AppContainer.processImageUseCase
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bitmap =
            BitmapLoader.loadDemoImage(this)

        // Get raw dimensions from resource header
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, R.drawable.roadster1, options)

        // Correctly get resource file size
        val fileSize = try {
            val assetFileDescriptor = resources.openRawResourceFd(R.drawable.roadster1)
            val size = assetFileDescriptor?.length ?: 0L
            assetFileDescriptor?.close()
            size
        } catch (e: Exception) {
            0L
        }

        viewModel.setBitmap(bitmap, options.outWidth, options.outHeight, fileSize)

        setContent {

            val uiState = viewModel.uiState.collectAsState()

            MaterialTheme {

                MainScreen(
                    uiState = uiState.value,
                    onBitmapSelected = { b, w, h, s ->
                        viewModel.setBitmap(b, w, h, s)
                    },
                    onProcessClick = {
                        viewModel.processImage()
                    }
                )
            }

        }

    }

}