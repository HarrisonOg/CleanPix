package com.harrisonog.cleanpix.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

data class TextOverlay(
    val text: String,
    val position: Offset,
    val color: Color,
    val textSize: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

data class TransformState(
    val offset: Offset = Offset.Zero,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var drawPaths by remember { mutableStateOf<List<DrawPath>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var textOverlays by remember { mutableStateOf<List<TextOverlay>>(emptyList()) }
    var showTextDialog by remember { mutableStateOf(false) }
    var drawingColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(10f) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var isDrawingMode by remember { mutableStateOf(false) }
    var selectedTextIndex by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        drawPaths = emptyList()
        textOverlays = emptyList()
        selectedTextIndex = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Editor") },
                actions = {
                    if (selectedImageUri != null) {
                        IconButton(onClick = { showTextDialog = true }) {
                            Icon(Icons.Default.Add, "Add Text")
                        }
                        IconButton(
                            onClick = {
                                isDrawingMode = !isDrawingMode
                                selectedTextIndex = null
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (isDrawingMode)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Create, "Draw Mode")
                        }
                        IconButton(onClick = {
                            drawPaths = emptyList()
                            textOverlays = emptyList()
                            selectedTextIndex = null
                        }) {
                            Icon(Icons.Default.Delete, "Clear")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedImageUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Select Image")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                imageSize = coordinates.size
                            },
                        contentScale = ContentScale.Fit
                    )

                    InteractiveCanvas(
                        drawPaths = drawPaths,
                        currentPath = currentPath,
                        textOverlays = textOverlays,
                        drawingColor = drawingColor,
                        strokeWidth = strokeWidth,
                        isDrawingMode = isDrawingMode,
                        selectedTextIndex = selectedTextIndex,
                        onDrawPathUpdate = { newPath ->
                            currentPath = newPath
                        },
                        onDrawPathComplete = { path ->
                            drawPaths = drawPaths + path
                            currentPath = emptyList()
                        },
                        onTextSelect = { index ->
                            selectedTextIndex = index
                            isDrawingMode = false
                        },
                        onTextUpdate = { index, updatedText ->
                            textOverlays = textOverlays.toMutableList().apply {
                                this[index] = updatedText
                            }
                        }
                    )
                }

                if (isDrawingMode) {
                    DrawingToolsCard(
                        strokeWidth = strokeWidth,
                        drawingColor = drawingColor,
                        onStrokeWidthChange = { strokeWidth = it },
                        onColorChange = { drawingColor = it }
                    )
                }

                if (selectedTextIndex != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Text selected - Use two fingers to rotate/scale",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    textOverlays = textOverlays.filterIndexed { index, _ ->
                                        index != selectedTextIndex
                                    }
                                    selectedTextIndex = null
                                }
                            ) {
                                Text("Delete Text")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Change Image")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                selectedImageUri?.let { uri ->
                                    saveEditedImage(
                                        context = context,
                                        originalUri = uri,
                                        imageSize = imageSize,
                                        drawPaths = drawPaths,
                                        textOverlays = textOverlays
                                    )
                                }
                            }
                        }
                    ) {
                        Text("Save Image")
                    }
                }
            }
        }
    }

    if (showTextDialog) {
        TextInputDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text ->
                textOverlays = textOverlays + TextOverlay(
                    text = text,
                    position = Offset(200f, 200f),
                    color = Color.White,
                    textSize = 60f,
                    rotation = 0f,
                    scale = 1f
                )
                showTextDialog = false
            }
        )
    }
}

@Composable
fun InteractiveCanvas(
    drawPaths: List<DrawPath>,
    currentPath: List<Offset>,
    textOverlays: List<TextOverlay>,
    drawingColor: Color,
    strokeWidth: Float,
    isDrawingMode: Boolean,
    selectedTextIndex: Int?,
    onDrawPathUpdate: (List<Offset>) -> Unit,
    onDrawPathComplete: (DrawPath) -> Unit,
    onTextSelect: (Int?) -> Unit,
    onTextUpdate: (Int, TextOverlay) -> Unit
) {
    var textTransforms by remember { mutableStateOf<Map<Int, TransformState>>(emptyMap()) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isDrawingMode, selectedTextIndex) {
                if (isDrawingMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onDrawPathUpdate(listOf(offset))
                        },
                        onDrag = { change, _ ->
                            onDrawPathUpdate(currentPath + change.position)
                        },
                        onDragEnd = {
                            if (currentPath.isNotEmpty()) {
                                onDrawPathComplete(
                                    DrawPath(
                                        points = currentPath,
                                        color = drawingColor,
                                        strokeWidth = strokeWidth
                                    )
                                )
                            }
                        }
                    )
                } else {
                    detectTapGestures { tapOffset ->
                        // Check if tap is on any text
                        val tappedIndex = textOverlays.indexOfLast { textOverlay ->
                            isPointInTextBounds(tapOffset, textOverlay)
                        }
                        onTextSelect(if (tappedIndex >= 0) tappedIndex else null)
                    }
                }
            }
            .pointerInput(selectedTextIndex) {
                if (selectedTextIndex != null && !isDrawingMode) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        val currentTransform = textTransforms[selectedTextIndex]
                            ?: TransformState()

                        val newTransform = TransformState(
                            offset = currentTransform.offset + pan,
                            rotation = currentTransform.rotation + rotation,
                            scale = (currentTransform.scale * zoom).coerceIn(0.5f, 3f)
                        )

                        textTransforms = textTransforms + (selectedTextIndex to newTransform)

                        val overlay = textOverlays[selectedTextIndex]
                        onTextUpdate(
                            selectedTextIndex,
                            overlay.copy(
                                position = overlay.position + pan,
                                rotation = overlay.rotation + rotation,
                                scale = (overlay.scale * zoom).coerceIn(0.5f, 3f)
                            )
                        )
                    }
                }
            }
    ) {
        // Draw paths
        drawPaths.forEach { path ->
            drawPath(
                path = path.points.toPath(),
                color = path.color,
                style = Stroke(width = path.strokeWidth, cap = StrokeCap.Round)
            )
        }

        if (currentPath.isNotEmpty()) {
            drawPath(
                path = currentPath.toPath(),
                color = drawingColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Draw text overlays
        textOverlays.forEachIndexed { index, textOverlay ->
            drawContext.canvas.save()

            // Apply transformations
            drawContext.canvas.translate(textOverlay.position.x, textOverlay.position.y)
            drawContext.canvas.rotate(textOverlay.rotation)
            drawContext.canvas.scale(textOverlay.scale, textOverlay.scale)

            val paint = Paint().apply {
                color = textOverlay.color.toArgb()
                textSize = textOverlay.textSize
                isAntiAlias = true
            }

            drawContext.canvas.nativeCanvas.drawText(
                textOverlay.text,
                0f,
                0f,
                paint
            )

            // Draw selection indicator
            if (index == selectedTextIndex) {
                val textWidth = paint.measureText(textOverlay.text)
                val textHeight = textOverlay.textSize

                drawContext.canvas.nativeCanvas.drawRect(
                    -10f,
                    -textHeight - 10f,
                    textWidth + 10f,
                    10f,
                    Paint().apply {
                        color = android.graphics.Color.BLUE
                        style = Paint.Style.STROKE
                        setStrokeWidth(3f)
                    }
                )
            }

            drawContext.canvas.restore()
        }
    }
}

@Composable
fun DrawingToolsCard(
    strokeWidth: Float,
    drawingColor: Color,
    onStrokeWidthChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Drawing Tools", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Stroke Width: ${strokeWidth.toInt()}px")
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 5f..50f
            )

            Text("Color")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(Color.Red, Color.Blue, Color.Green, Color.Black, Color.Yellow)
                    .forEach { color ->
                        ColorButton(
                            color = color,
                            isSelected = drawingColor == color,
                            onClick = { onColorChange(color) }
                        )
                    }
            }
        }
    }
}

fun isPointInTextBounds(point: Offset, textOverlay: TextOverlay): Boolean {
    val paint = Paint().apply {
        textSize = textOverlay.textSize * textOverlay.scale
    }
    val textWidth = paint.measureText(textOverlay.text)
    val textHeight = textOverlay.textSize * textOverlay.scale

    // Simple bounding box check (doesn't account for rotation perfectly)
    return point.x >= textOverlay.position.x - 20 &&
            point.x <= textOverlay.position.x + textWidth + 20 &&
            point.y >= textOverlay.position.y - textHeight - 20 &&
            point.y <= textOverlay.position.y + 20
}

suspend fun PointerInputScope.detectTransformGestures(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown()

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }

            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val zoomMotion = kotlin.math.abs(1 - zoom)
                    val rotationMotion = kotlin.math.abs(rotation)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > 0.1f ||
                        rotationMotion > 0.1f ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.changes.fold(Offset.Zero) { acc, c ->
                        acc + c.position
                    } / event.changes.size.toFloat()

                    if (zoomChange != 1f ||
                        rotationChange != 0f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, rotationChange)
                    }
                    event.changes.forEach {
                        if (it.position != it.previousPosition) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

@Composable
fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.size(50.dp),
        contentPadding = PaddingValues(0.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {}
}

@Composable
fun TextInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Text") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter text") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun List<Offset>.toPath(): Path {
    val path = Path()
    if (isEmpty()) return path

    path.moveTo(first().x, first().y)
    for (i in 1 until size) {
        path.lineTo(this[i].x, this[i].y)
    }
    return path
}

suspend fun saveEditedImage(
    context: Context,
    originalUri: Uri,
    imageSize: IntSize,
    drawPaths: List<DrawPath>,
    textOverlays: List<TextOverlay>
) = withContext(Dispatchers.IO) {
    try {
        val originalBitmap = context.contentResolver.openInputStream(originalUri)?.use {
            android.graphics.BitmapFactory.decodeStream(it)
        } ?: return@withContext

        val editedBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(editedBitmap)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        val scaleX = originalBitmap.width.toFloat() / imageSize.width
        val scaleY = originalBitmap.height.toFloat() / imageSize.height

        drawPaths.forEach { path ->
            val paint = Paint().apply {
                color = path.color.toArgb()
                strokeWidth = path.strokeWidth * scaleX
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }

            val scaledPath = android.graphics.Path()
            path.points.forEachIndexed { index, offset ->
                val scaledX = offset.x * scaleX
                val scaledY = offset.y * scaleY
                if (index == 0) {
                    scaledPath.moveTo(scaledX, scaledY)
                } else {
                    scaledPath.lineTo(scaledX, scaledY)
                }
            }
            canvas.drawPath(scaledPath, paint)
        }

        textOverlays.forEach { textOverlay ->
            val paint = Paint().apply {
                color = textOverlay.color.toArgb()
                textSize = textOverlay.textSize * scaleX
                isAntiAlias = true
            }
            canvas.drawText(
                textOverlay.text,
                textOverlay.position.x * scaleX,
                textOverlay.position.y * scaleY,
                paint
            )
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "edited_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ImageEditor")
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                editedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Image saved successfully!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        editedBitmap.recycle()
        originalBitmap.recycle()
    } catch (e: IOException) {
        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(
                context,
                "Error saving image: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
