package com.kouhee.imagebenchmark

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import com.kouhee.imagebenchmark.data.loader.BitmapLoader
import com.kouhee.imagebenchmark.di.AppContainer
import com.kouhee.imagebenchmark.presentation.screen.MainScreen
import com.kouhee.imagebenchmark.presentation.viewmodel.MainViewModel
import java.io.IOException

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    private external fun stringFromJNI(): String

    private val viewModel = MainViewModel(
        AppContainer.processImageUseCase
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setJniString(stringFromJNI())

        val bitmap = BitmapLoader.loadDemoImage(this)

        // Get raw dimensions and file size from assets
        val fileName = "roadster1.jpg"
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        
        var fileSize = 0L
        try {
            assets.open(fileName).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            assets.openFd(fileName).use { fd ->
                fileSize = fd.length
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        viewModel.setBitmap(bitmap, options.outWidth, options.outHeight, fileSize)

        setContent {
            val uiState = viewModel.uiState.collectAsState()
            MaterialTheme {
                MainScreen(
                    uiState = uiState.value,
                    onUriSelected = { uri ->
                        viewModel.loadImage(contentResolver, uri)
                    },
                    onFilterSelected = { filter ->
                        viewModel.setFilter(filter)
                    },
                    onEngineSelected = { engine ->
                        viewModel.setEngine(engine)
                    },
                    onProcessClick = {
                        viewModel.processImage()
                    }
                )
            }
        }
    }
}