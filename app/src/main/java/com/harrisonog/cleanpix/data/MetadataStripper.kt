package com.harrisonog.cleanpix.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for reading and stripping metadata from images
 */
class MetadataStripper(private val context: Context) {

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
            // Load the bitmap from the original URI
            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return Result.failure(Exception("Failed to load image"))

            // Create a temporary file for the cleaned image
            val cleanedFile = File(context.cacheDir, "cleaned_${System.currentTimeMillis()}.jpg")

            // Save bitmap without metadata (JPEG compression creates a new file without EXIF)
            FileOutputStream(cleanedFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }

            Result.success(Uri.fromFile(cleanedFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save cleaned image to permanent storage (Pictures directory)
     */
    fun saveToPermanentStorage(tempUri: Uri, fileName: String): Result<File> {
        return try {
            val picturesDir = File(context.getExternalFilesDir(null), "CleanedImages")
            picturesDir.mkdirs()

            val outputFile = File(picturesDir, fileName)

            context.contentResolver.openInputStream(tempUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

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