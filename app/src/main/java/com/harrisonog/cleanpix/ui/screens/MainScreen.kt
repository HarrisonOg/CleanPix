package com.harrisonog.cleanpix.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.harrisonog.cleanpix.ui.ImageState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: ImageState,
    onImageSelected: (Uri) -> Unit,
    onStripMetadata: () -> Unit,
    onSaveImage: () -> Unit,
    onClearState: () -> Unit,
    onDismissError: () -> Unit,
    onDismissSaved: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onImageSelected)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metadata Stripper") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Select Image Button
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Image")
            }

            // Show original image and metadata
            if (state.originalUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Original Image",
                            style = MaterialTheme.typography.titleMedium
                        )

                        AsyncImage(
                            model = state.originalUri,
                            contentDescription = "Original image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )

                        if (state.originalMetadata.isNotEmpty()) {
                            Text(
                                text = "Metadata Found:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )

                            state.originalMetadata.forEach { (key, value) ->
                                Text(
                                    text = "$key: $value",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                text = "No metadata found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Strip Metadata Button
                Button(
                    onClick = onStripMetadata,
                    enabled = !state.isProcessing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Strip Metadata")
                }
            }

            // Show cleaned image
            if (state.cleanedUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✓ Cleaned Image",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AsyncImage(
                            model = state.cleanedUri,
                            contentDescription = "Cleaned image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )

                        if (state.cleanedMetadata.isEmpty()) {
                            Text(
                                text = "✓ All metadata successfully removed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Warning: Some metadata remains:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            state.cleanedMetadata.forEach { (key, value) ->
                                Text(
                                    text = "$key: $value",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearState,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Over")
                    }

                    Button(
                        onClick = onSaveImage,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isProcessing
                    ) {
                        Text("Save Image")
                    }
                }
            }

            // Error message
            state.error?.let { error ->
                AlertDialog(
                    onDismissRequest = onDismissError,
                    title = { Text("Error") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = onDismissError) {
                            Text("OK")
                        }
                    }
                )
            }

            // Success message
            state.savedPath?.let { path ->
                AlertDialog(
                    onDismissRequest = onDismissSaved,
                    title = { Text("Success") },
                    text = { Text("Image saved to:\n$path") },
                    confirmButton = {
                        TextButton(onClick = onDismissSaved) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}