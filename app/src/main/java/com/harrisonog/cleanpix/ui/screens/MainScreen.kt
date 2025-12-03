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
    onSaveImage: (String) -> Unit,
    onClearState: () -> Unit,
    onDismissError: () -> Unit,
    onDismissSaved: () -> Unit,
    onEditImage: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    var showFilenameDialog by remember { mutableStateOf(false) }
    var customFilename by remember { mutableStateOf("") }

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
            when {
                // No image selected - Show only "Choose Image" button
                state.originalUri == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.button_choose_image))
                        }
                    }
                }

                // Image selected but metadata not cleaned yet
                !state.hasCleanedMetadata -> {
                    // Show image and metadata
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsyncImage(
                                model = state.originalUri,
                                contentDescription = stringResource(R.string.content_desc_original_image),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentScale = ContentScale.Fit
                            )

                            if (state.originalMetadata.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.metadata_found),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )

                                for ((key, value) in state.originalMetadata.entries.take(5)) {
                                    Text(
                                        text = "$key: $value",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (state.originalMetadata.size > 5) {
                                    Text(
                                        text = "... and ${state.originalMetadata.size - 5} more",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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

                    // Clean Metadata and Edit Image buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStripMetadata,
                            enabled = !state.isProcessing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.button_clean_metadata))
                            }
                        }

                        Button(
                            onClick = {
                                state.originalUri?.let { uri ->
                                    onEditImage(uri)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.button_edit_image))
                        }
                    }

                    // Start Over button
                    OutlinedButton(
                        onClick = onClearState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.button_start_over))
                    }
                }

                // Metadata has been cleaned - Show cleaned image and action buttons
                else -> {
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
                                    .height(300.dp),
                                contentScale = ContentScale.Fit
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
                                for ((key, value) in state.cleanedMetadata.entries) {
                                    Text(
                                        text = "$key: $value",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.button_share))
                            }

                            Button(
                                onClick = {
                                    state.cleanedUri?.let { uri ->
                                        onEditImage(uri)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.button_edit_image))
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
                                    customFilename = "cleaned_${System.currentTimeMillis()}"
                                    showFilenameDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isProcessing
                            ) {
                                Text(stringResource(R.string.button_save_image))
                            }
                        }
                    }
                }
            }

            // Filename input dialog
            if (showFilenameDialog) {
                AlertDialog(
                    onDismissRequest = { showFilenameDialog = false },
                    title = { Text(stringResource(R.string.dialog_title_save_image)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.dialog_message_enter_filename))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customFilename,
                                onValueChange = { customFilename = it },
                                label = { Text(stringResource(R.string.label_filename)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customFilename.isNotBlank()) {
                                    onSaveImage(customFilename.trim())
                                    showFilenameDialog = false
                                }
                            }
                        ) {
                            Text(stringResource(R.string.button_save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFilenameDialog = false }) {
                            Text(stringResource(R.string.button_cancel))
                        }
                    }
                )
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