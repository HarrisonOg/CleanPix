package com.harrisonog.cleanpix.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.harrisonog.cleanpix.R
import com.harrisonog.cleanpix.ui.ImageState
import java.io.File

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
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onImageSelected)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_metadata_stripper)) },
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
                Text(stringResource(R.string.button_select_image))
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
                            text = stringResource(R.string.label_original_image),
                            style = MaterialTheme.typography.titleMedium
                        )

                        AsyncImage(
                            model = state.originalUri,
                            contentDescription = stringResource(R.string.content_desc_original_image),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )

                        if (state.originalMetadata.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.metadata_found),
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
                                text = stringResource(R.string.no_metadata_found),
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
                    Text(stringResource(R.string.button_strip_metadata))
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
                            text = stringResource(R.string.label_cleaned_image),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AsyncImage(
                            model = state.cleanedUri,
                            contentDescription = stringResource(R.string.content_desc_cleaned_image),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )

                        if (state.cleanedMetadata.isEmpty()) {
                            Text(
                                text = stringResource(R.string.metadata_removed_success),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.warning_metadata_remains),
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
                        Text(stringResource(R.string.button_start_over))
                    }

                    Button(
                        onClick = {
                            state.cleanedUri?.let { uri ->
                                try {
                                    // Convert file:// URI to content:// URI using FileProvider
                                    val contentUri = if (uri.scheme == "file") {
                                        val file = File(uri.path ?: return@let)
                                        FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                    } else {
                                        uri
                                    }

                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        type = "image/*"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, context.getString(R.string.share_image))
                                    )
                                } catch (e: Exception) {
                                    // Handle error silently or show a toast
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to share image: ${e.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.button_share),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = onSaveImage,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isProcessing
                    ) {
                        Text(stringResource(R.string.button_save_image))
                    }
                }
            }

            // Error message
            state.error?.let { error ->
                AlertDialog(
                    onDismissRequest = onDismissError,
                    title = { Text(stringResource(R.string.dialog_title_error)) },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = onDismissError) {
                            Text(stringResource(R.string.button_ok))
                        }
                    }
                )
            }

            // Success message
            state.savedPath?.let { path ->
                AlertDialog(
                    onDismissRequest = onDismissSaved,
                    title = { Text(stringResource(R.string.dialog_title_success)) },
                    text = { Text(stringResource(R.string.image_saved_to, path)) },
                    confirmButton = {
                        TextButton(onClick = onDismissSaved) {
                            Text(stringResource(R.string.button_ok))
                        }
                    }
                )
            }
        }
    }
}