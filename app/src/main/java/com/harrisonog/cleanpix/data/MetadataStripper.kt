package com.harrisonog.cleanpix.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for reading and stripping metadata from images
 */
class MetadataStripper(private val context: Context) {

    companion object {
        private const val MAX_FILE_SIZE_MB = 25 // Maximum file size in MB
        private const val MAX_IMAGE_DIMENSION = 8192 // Maximum width or height in pixels

        private val SUPPORTED_MIME_TYPES = listOf(
            "image/jpeg",
            "image/jpg",
            "image/png"
        )
    }

    /**
     * Get the file size of an image URI in bytes
     */
    private fun getFileSize(uri: Uri): Long? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the MIME type of an image URI
     */
    private fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * Get image dimensions without loading the full bitmap into memory
     */
    private fun getImageDimensions(uri: Uri): Pair<Int, Int>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                Pair(options.outWidth, options.outHeight)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validate image file before processing
     * Checks MIME type, file size, and dimensions
     */
    private fun validateImage(uri: Uri): Result<Unit> {
        // 1. Check MIME type (if available)
        // Note: MIME type might be null for programmatically created files or file:// URIs
        val mimeType = getMimeType(uri)
        if (mimeType != null && mimeType !in SUPPORTED_MIME_TYPES) {
            // If we have a MIME type and it's not supported, reject it
            return Result.failure(Exception("Unsupported image format. Please select a JPG or PNG image."))
        }

        // 2. Check file size
        val fileSize = getFileSize(uri)
        if (fileSize != null) {
            val fileSizeMB = fileSize / (1024.0 * 1024.0)
            if (fileSizeMB > MAX_FILE_SIZE_MB) {
                return Result.failure(Exception("Image file is too large (${String.format("%.1f", fileSizeMB)}MB). Maximum size is ${MAX_FILE_SIZE_MB}MB."))
            }
        }
        // Note: If file size is null, we continue (might be a stream or special URI)

        // 3. Check dimensions
        val dimensions = getImageDimensions(uri)
        if (dimensions != null) {
            val (width, height) = dimensions
            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                return Result.failure(Exception("Image dimensions too large (${width}x${height}px). Maximum dimension is ${MAX_IMAGE_DIMENSION}px."))
            }
            if (width <= 0 || height <= 0) {
                return Result.failure(Exception("Invalid image dimensions."))
            }
        }
        // Note: If dimensions are null, we let BitmapFactory handle it and fail later if needed

        return Result.success(Unit)
    }

    /**
     * Read EXIF metadata from an image URI
     */
    fun readMetadata(uri: Uri): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)

                // Common EXIF tags
                val tags = listOf(
                    ExifInterface.TAG_DATETIME to "Date/Time",
                    ExifInterface.TAG_MAKE to "Camera Make",
                    ExifInterface.TAG_MODEL to "Camera Model",
                    ExifInterface.TAG_GPS_LATITUDE to "GPS Latitude",
                    ExifInterface.TAG_GPS_LONGITUDE to "GPS Longitude",
                    ExifInterface.TAG_GPS_ALTITUDE to "GPS Altitude",
                    ExifInterface.TAG_ORIENTATION to "Orientation",
                    ExifInterface.TAG_SOFTWARE to "Software",
                    ExifInterface.TAG_IMAGE_WIDTH to "Width",
                    ExifInterface.TAG_IMAGE_LENGTH to "Height",
                    ExifInterface.TAG_ARTIST to "Artist",
                    ExifInterface.TAG_COPYRIGHT to "Copyright",
                    ExifInterface.TAG_GPS_DATESTAMP to "GPS Date",
                    ExifInterface.TAG_GPS_TIMESTAMP to "GPS Time",
                    ExifInterface.TAG_APERTURE_VALUE to "Aperture",
                    ExifInterface.TAG_ISO_SPEED to "ISO Speed",
                    ExifInterface.TAG_FOCAL_LENGTH to "Focal Length"
                )

                tags.forEach { (tag, displayName) ->
                    exif.getAttribute(tag)?.let { value ->
                        metadata[displayName] = value
                    }
                }

                // Get GPS coordinates if available
                val latLong = exif.latLong
                if (latLong != null) {
                    metadata["GPS Coordinates"] = "${latLong[0]}, ${latLong[1]}"
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return metadata
    }

    /**
     * Strip all metadata from an image and save to a new file
     * Returns the URI of the cleaned image
     */
    fun stripMetadata(uri: Uri): Result<Uri> {
        return try {
            // Validate image before processing
            validateImage(uri).getOrElse { exception ->
                return Result.failure(exception)
            }

            // Read EXIF orientation before loading bitmap
            val orientation = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            // Load the bitmap from the original URI
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return Result.failure(Exception("Failed to load image. The file may be corrupted."))

            // Rotate bitmap based on EXIF orientation
            val rotatedBitmap = rotateBitmap(originalBitmap, orientation)

            // Create a temporary file for the cleaned image
            val cleanedFile = File(context.cacheDir, "cleaned_${System.currentTimeMillis()}.jpg")

            // Save bitmap without metadata (JPEG compression creates a new file without EXIF)
            FileOutputStream(cleanedFile).use { outputStream ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }

            // Clean up bitmaps if rotation created a new one
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            Result.success(Uri.fromFile(cleanedFile))
        } catch (e: OutOfMemoryError) {
            Result.failure(Exception("Image is too large to process. Please try a smaller image."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rotate a bitmap based on EXIF orientation
     */
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap // ORIENTATION_NORMAL or ORIENTATION_UNDEFINED
        }

        return try {
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            rotatedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Save cleaned image to permanent storage (public Pictures directory)
     */
    fun saveToPermanentStorage(tempUri: Uri, fileName: String): Result<File> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): Use MediaStore API
                saveToPicturesMediaStore(tempUri, fileName)
            } else {
                // Android 9 and below: Use legacy external storage
                saveToPicturesLegacy(tempUri, fileName)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save to public Pictures directory using MediaStore (Android 10+)
     */
    private fun saveToPicturesMediaStore(tempUri: Uri, fileName: String): Result<File> {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CleanPix")
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(Exception("Failed to create MediaStore entry"))

            resolver.openOutputStream(imageUri)?.use { output ->
                resolver.openInputStream(tempUri)?.use { input ->
                    input.copyTo(output)
                }
            }

            // Return a file representation (note: actual path may not be accessible on Android 10+)
            val picturesPath = "${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_PICTURES}/CleanPix/$fileName"
            Result.success(File(picturesPath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save to public Pictures directory using legacy method (Android 9 and below)
     */
    private fun saveToPicturesLegacy(tempUri: Uri, fileName: String): Result<File> {
        return try {
            val picturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "CleanPix"
            )
            picturesDir.mkdirs()

            val outputFile = File(picturesDir, fileName)

            context.contentResolver.openInputStream(tempUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Notify the media scanner about the new file
            val scanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            scanIntent.data = Uri.fromFile(outputFile)
            context.sendBroadcast(scanIntent)

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify that an image has no EXIF metadata
     */
    fun verifyNoMetadata(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                // Check if common tags are present
                exif.getAttribute(ExifInterface.TAG_DATETIME) == null &&
                        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null &&
                        exif.getAttribute(ExifInterface.TAG_MAKE) == null
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}