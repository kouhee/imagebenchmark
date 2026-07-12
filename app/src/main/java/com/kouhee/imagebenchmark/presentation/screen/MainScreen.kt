package com.kouhee.imagebenchmark.presentation.screen

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kouhee.imagebenchmark.domain.model.FilterType
import com.kouhee.imagebenchmark.domain.model.ProcessingEngine
import com.kouhee.imagebenchmark.presentation.state.MainUiState

@Composable
fun MainScreen(
    uiState: MainUiState,
    onUriSelected: (Uri) -> Unit,
    onFilterSelected: (FilterType) -> Unit,
    onEngineSelected: (ProcessingEngine) -> Unit,
    onProcessClick: () -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onUriSelected) }
    val supportedEngines = uiState.selectedFilter.supportedEngines()

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
                val isSupported = engine in supportedEngines
                Row(
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (isSupported) 1f else 0.45f)
                        .selectable(
                            enabled = isSupported,
                            selected = (uiState.selectedEngine == engine),
                            onClick = { onEngineSelected(engine) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        enabled = isSupported,
                        selected = (uiState.selectedEngine == engine),
                        onClick = null
                    )
                    Text(
                        text = if (isSupported) engine.displayName() else "${engine.displayName()} (未対応)"
                    )
                }
            }
        }

        if (uiState.originalWidth > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Original File: ${uiState.originalWidth} x ${uiState.originalHeight} (${formatBytes(uiState.originalFileSize)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        uiState.inputBitmap?.let { bitmap ->
            val memorySize = bitmap.width * bitmap.height * 4L
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Memory: ${bitmap.width} x ${bitmap.height} (${formatBytes(memorySize)})",
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

        Text(
            text = "Fastest (${uiState.selectedEngine.displayName()}) : ${formatTimeUs(uiState.fastestTimeUs)}",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Slowest (${uiState.selectedEngine.displayName()}) : ${formatTimeUs(uiState.slowestTimeUs)}",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Available CPU cores: ${uiState.threadCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
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
private fun ImagePanel(title: String, bitmap: Bitmap?, modifier: Modifier) {
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

private fun formatBytes(sizeInBytes: Long): String {
    val sizeInKB = sizeInBytes / 1024.0
    val sizeInMB = sizeInKB / 1024.0
    return if (sizeInMB >= 1.0) "%.2f MB".format(sizeInMB) else "%.2f KB".format(sizeInKB)
}

private fun formatTimeUs(timeUs: Double?): String {
    return if (timeUs == null) "--" else "%.3f μs".format(timeUs)
}