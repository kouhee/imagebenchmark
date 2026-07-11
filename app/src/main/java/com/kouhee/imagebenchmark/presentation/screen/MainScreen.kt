package com.kouhee.imagebenchmark.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kouhee.imagebenchmark.presentation.state.MainUiState
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext


@Composable
fun MainScreen(
    uiState: MainUiState,
    onBitmapSelected: (Bitmap, Int, Int, Long) -> Unit,
    onFilterSelected: (FilterType) -> Unit,
    onEngineSelected: (ProcessingEngine) -> Unit,
    onProcessClick: () -> Unit
) {

    val context = LocalContext.current

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->  uri ?: return@rememberLauncherForActivityResult

            // Get original dimensions and file size directly from JPG header
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var originalWidth = 0
            var originalHeight = 0

            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
                
                // Re-open stream for ExifInterface to handle rotation
                context.contentResolver.openInputStream(uri)?.use { exifInput ->
                    val exif = ExifInterface(exifInput)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || 
                        orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        originalWidth = options.outHeight
                        originalHeight = options.outWidth
                    } else {
                        originalWidth = options.outWidth
                        originalHeight = options.outHeight
                    }
                }
            }
            
            val fileSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { 
                it.length
            } ?: 0L

            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }

            onBitmapSelected(bitmap, originalWidth, originalHeight, fileSize)
        }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Performance Lab",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        if (uiState.jniString.isNotEmpty()) {
            Text(
                text = uiState.jniString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {

                photoPicker.launch(

                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )

                )

            }
        ) {
            Text("画像を選択")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Filter :", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.selectableGroup()) {
            FilterType.entries.forEach { filter ->
                Row(
                    Modifier
                        .selectable(
                            selected = (uiState.selectedFilter == filter),
                            onClick = { onFilterSelected(filter) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (uiState.selectedFilter == filter),
                        onClick = null // null recommended for accessibility with screen readers
                    )
                    Text(text = filter.displayName())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Engine :", style = MaterialTheme.typography.titleSmall)
        Column(Modifier.selectableGroup()) {
            ProcessingEngine.entries.forEach { engine ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (uiState.selectedEngine == engine),
                            onClick = { onEngineSelected(engine) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (uiState.selectedEngine == engine),
                        onClick = null
                    )
                    Text(text = engine.displayName())
                }
            }
        }

        if (uiState.originalWidth > 0) {
            val originalKB = uiState.originalFileSize / 1024.0
            val originalMB = originalKB / 1024.0
            val originalSizeText = if (originalMB >= 1.0) "%.2f MB".format(originalMB) else "%.2f KB".format(originalKB)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Original File: ${uiState.originalWidth} x ${uiState.originalHeight} ($originalSizeText)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        uiState.inputBitmap?.let { bitmap ->
            // Memory size should reflect the IntArray pixel data (4 bytes per pixel)
            val sizeInBytes = bitmap.width * bitmap.height * 4L
            val sizeInKB = sizeInBytes / 1024.0
            val sizeInMB = sizeInKB / 1024.0
            val sizeText = if (sizeInMB >= 1.0) "%.2f MB".format(sizeInMB) else "%.2f KB".format(sizeInKB)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Memory: ${bitmap.width} x ${bitmap.height} ($sizeText)",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onProcessClick,
        ) {
            Text("実行")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Elapsed : %.3f μs".format(uiState.elapsedTimeUs),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        ImagePanel(
            title = "BEFORE",
            bitmap = uiState.inputBitmap,
            modifier = Modifier.weight(1f)
        )
        
        ImagePanel(
            title = "AFTER",
            bitmap = uiState.outputBitmap,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ImagePanel(title: String, bitmap: Bitmap?, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        } else {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}