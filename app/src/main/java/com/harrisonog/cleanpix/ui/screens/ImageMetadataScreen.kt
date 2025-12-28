package com.harrisonog.cleanpix.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
fun ImageMetadataScreen(
    state: ImageState,
    onStripMetadata: () -> Unit,
    onSaveImage: (String) -> Unit,
    onCancel: () -> Unit,
    onStartOver: () -> Unit,
    onDismissError: () -> Unit,
    onDismissSaved: () -> Unit
) {
    val context = LocalContext.current
    var showFileNameDialog by rememberSaveable { mutableStateOf(false) }
    var fileName by rememberSaveable { mutableStateOf("cleaned_${System.currentTimeMillis()}") }
    var isMetadataExpanded by rememberSaveable { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isMetadataExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow rotation"
    )

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
            // Show original image and metadata (only if metadata hasn't been cleaned yet)
            if (state.originalUri != null && state.cleanedUri == null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Image section with padding
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
                                    .height(300.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Expandable metadata section - full width clickable area
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { isMetadataExpanded = !isMetadataExpanded }
                        ) {
                            Column {
                                // Clickable header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isMetadataExpanded) {
                                            stringResource(R.string.button_close_metadata)
                                        } else {
                                            stringResource(R.string.button_show_metadata)
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(arrowRotation)
                                    )
                                }
                            }
                        }

                        // Expandable content with animation
                        AnimatedVisibility(
                            visible = isMetadataExpanded,
                            enter = expandVertically(
                                animationSpec = tween(durationMillis = 300)
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 300)
                            ),
                            exit = shrinkVertically(
                                animationSpec = tween(durationMillis = 300)
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 300)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                    }
                }

                // Clean Metadata and Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !state.isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.button_cancel),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.button_cancel))
                    }

                    Button(
                        onClick = onStripMetadata,
                        enabled = !state.isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = stringResource(R.string.button_strip_metadata),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.button_strip_metadata))
                    }
                }
            }

            // Show cleaned image after metadata is stripped
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
                                .height(300.dp),
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

                // Share and Save buttons (shown after metadata is cleaned)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            state.cleanedUri.let { uri ->
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
                                        context.getString(R.string.error_share_image, e.message ?: "Unknown error"),
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
                            fileName = "cleaned_${System.currentTimeMillis()}"
                            showFileNameDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.button_save_image),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.button_save_image))
                    }
                }

                // Start Over button
                OutlinedButton(
                    onClick = onStartOver,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.button_start_over),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_start_over))
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

            // File name dialog
            if (showFileNameDialog) {
                AlertDialog(
                    onDismissRequest = { showFileNameDialog = false },
                    title = { Text(stringResource(R.string.dialog_title_save_image)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.dialog_message_file_name))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = fileName,
                                onValueChange = { fileName = it },
                                label = { Text(stringResource(R.string.label_file_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showFileNameDialog = false
                                onSaveImage(fileName)
                            }
                        ) {
                            Text(stringResource(R.string.button_save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFileNameDialog = false }) {
                            Text(stringResource(R.string.button_cancel))
                        }
                    }
                )
            }
        }
    }
}
