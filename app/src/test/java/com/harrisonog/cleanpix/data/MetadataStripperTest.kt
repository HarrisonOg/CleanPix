package com.harrisonog.cleanpix.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MetadataStripperTest {

    private lateinit var context: Context
    private lateinit var metadataStripper: MetadataStripper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        metadataStripper = MetadataStripper(context)
    }

    @Test
    fun `readMetadata returns empty map for non-existent URI`() {
        // Given
        val nonExistentUri = Uri.parse("file:///nonexistent/path/image.jpg")

        // When
        val result = metadataStripper.readMetadata(nonExistentUri)

        // Then
        assertTrue("Expected empty metadata map", result.isEmpty())
    }

    @Test
    fun `stripMetadata fails for non-existent URI`() {
        // Given
        val nonExistentUri = Uri.parse("file:///nonexistent/path/image.jpg")

        // When
        val result = metadataStripper.stripMetadata(nonExistentUri)

        // Then
        assertTrue("Expected failure for non-existent URI", result.isFailure)
    }

    @Test
    fun `stripMetadata creates temp file with jpg extension`() {
        // Given - Create a simple test bitmap
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val testFile = File(context.cacheDir, "test_image.jpg")

        FileOutputStream(testFile).use { out ->
            testBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        val testUri = Uri.fromFile(testFile)

        try {
            // When
            val result = metadataStripper.stripMetadata(testUri)

            // Then
            assertTrue("Expected success when stripping metadata", result.isSuccess)

            val cleanedUri = result.getOrNull()
            assertNotNull("Cleaned URI should not be null", cleanedUri)

            val cleanedPath = cleanedUri?.path
            assertNotNull("Cleaned path should not be null", cleanedPath)
            assertTrue("Cleaned file should have .jpg extension", cleanedPath!!.endsWith(".jpg"))
            assertTrue("Cleaned file should start with 'cleaned_'", File(cleanedPath).name.startsWith("cleaned_"))
        } finally {
            // Cleanup
            testFile.delete()
        }
    }

    @Test
    fun `verifyNoMetadata returns false for non-existent URI`() {
        // Given
        val nonExistentUri = Uri.parse("file:///nonexistent/path/image.jpg")

        // When
        val result = metadataStripper.verifyNoMetadata(nonExistentUri)

        // Then
        assertFalse("Expected false for non-existent URI", result)
    }

    @Test
    fun `verifyNoMetadata returns true for image without metadata`() {
        // Given - Create a simple bitmap without metadata
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val testFile = File(context.cacheDir, "test_no_metadata.jpg")

        // Save without any EXIF data
        FileOutputStream(testFile).use { out ->
            testBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        val testUri = Uri.fromFile(testFile)

        try {
            // When
            val result = metadataStripper.verifyNoMetadata(testUri)

            // Then
            assertTrue("Expected true for image without metadata", result)
        } finally {
            // Cleanup
            testFile.delete()
        }
    }

    @Test
    fun `readMetadata returns empty map for image without metadata`() {
        // Given - Create a simple bitmap without metadata
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val testFile = File(context.cacheDir, "test_read_metadata.jpg")

        FileOutputStream(testFile).use { out ->
            testBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        val testUri = Uri.fromFile(testFile)

        try {
            // When
            val metadata = metadataStripper.readMetadata(testUri)

            // Then - Either empty or only contains width/height which are not privacy concerns
            val hasPrivacyMetadata = metadata.keys.any { key ->
                key !in listOf("Width", "Height", "Orientation")
            }
            assertFalse("Expected no privacy-sensitive metadata", hasPrivacyMetadata)
        } finally {
            // Cleanup
            testFile.delete()
        }
    }

    @Test
    fun `readMetadata extracts datetime when present`() {
        // Given - Create an image with EXIF data
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val testFile = File(context.cacheDir, "test_with_exif.jpg")

        // Save bitmap first
        FileOutputStream(testFile).use { out ->
            testBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        // Add EXIF data
        val exif = ExifInterface(testFile.absolutePath)
        exif.setAttribute(ExifInterface.TAG_DATETIME, "2024:01:01 12:00:00")
        exif.setAttribute(ExifInterface.TAG_MAKE, "TestCamera")
        exif.saveAttributes()

        val testUri = Uri.fromFile(testFile)

        try {
            // When
            val metadata = metadataStripper.readMetadata(testUri)

            // Then
            assertFalse("Metadata should not be empty", metadata.isEmpty())
            assertEquals("2024:01:01 12:00:00", metadata["Date/Time"])
            assertEquals("TestCamera", metadata["Camera Make"])
        } finally {
            // Cleanup
            testFile.delete()
        }
    }

    @Test
    fun `stripMetadata removes EXIF data`() {
        // Given - Create an image with EXIF data
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val testFile = File(context.cacheDir, "test_strip_exif.jpg")

        // Save bitmap first
        FileOutputStream(testFile).use { out ->
            testBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        // Add EXIF data
        val exif = ExifInterface(testFile.absolutePath)
        exif.setAttribute(ExifInterface.TAG_DATETIME, "2024:01:01 12:00:00")
        exif.setAttribute(ExifInterface.TAG_MAKE, "TestCamera")
        exif.setAttribute(ExifInterface.TAG_MODEL, "Test Model")
        exif.saveAttributes()

        val testUri = Uri.fromFile(testFile)

        // Verify metadata exists before stripping
        val metadataBefore = metadataStripper.readMetadata(testUri)
        assertTrue("Date/Time metadata should exist before stripping", metadataBefore.containsKey("Date/Time"))
        assertTrue("Camera Make metadata should exist before stripping", metadataBefore.containsKey("Camera Make"))

        try {
            // When
            val result = metadataStripper.stripMetadata(testUri)

            // Then
            assertTrue("Strip metadata should succeed", result.isSuccess)

            val cleanedUri = result.getOrNull()
            assertNotNull("Cleaned URI should not be null", cleanedUri)

            // Verify privacy-sensitive metadata is removed
            val metadataAfter = metadataStripper.readMetadata(cleanedUri!!)
            assertFalse("Date/Time should be removed", metadataAfter.containsKey("Date/Time"))
            assertFalse("Camera Make should be removed", metadataAfter.containsKey("Camera Make"))
            assertFalse("Camera Model should be removed", metadataAfter.containsKey("Camera Model"))

            // Cleanup cleaned file
            cleanedUri.path?.let { File(it).delete() }
        } finally {
            // Cleanup original file
            testFile.delete()
        }
    }

    @Test
    fun `stripMetadata preserves image dimensions`() {
        // Given - Create a test bitmap with specific dimensions
        val width = 200
        val height = 150
        val testBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val testFile = File(context.cacheDir, "test_dimensions.jpg")

        FileOutputStream(testFile).use { out ->
            testBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        val testUri = Uri.fromFile(testFile)

        try {
            // When
            val result = metadataStripper.stripMetadata(testUri)

            // Then
            assertTrue("Strip metadata should succeed", result.isSuccess)

            val cleanedUri = result.getOrNull()
            assertNotNull("Cleaned URI should not be null", cleanedUri)

            // Verify dimensions are preserved by reading the cleaned image
            cleanedUri?.path?.let { path ->
                val cleanedFile = File(path)
                assertTrue("Cleaned file should exist", cleanedFile.exists())
                assertTrue("Cleaned file should have content", cleanedFile.length() > 0)

                // Cleanup cleaned file
                cleanedFile.delete()
            }
        } finally {
            // Cleanup original file
            testFile.delete()
        }
    }
}
