# CleanPix

A privacy-focused Android application for removing metadata from images. CleanPix (officially "Picture Information Cleaner") helps users protect their privacy by stripping EXIF data from photos before sharing them.

## Overview

CleanPix is a lightweight, offline-first Android app that allows users to view and remove metadata from their images. The app displays comprehensive EXIF information including GPS coordinates, camera details, timestamps, and other identifying data, then creates cleaned copies of images with all metadata removed.

## Key Features

- **Metadata Viewing**: Display comprehensive EXIF data including GPS coordinates, camera make/model, timestamps, ISO, aperture, focal length, and more
- **Complete Privacy**: Strip all EXIF metadata from images with a single tap
- **Offline Operation**: Fully functional without internet connectivity - no data leaves your device
- **Image Processing**: Automatically handles image rotation based on EXIF orientation
- **Flexible Saving**: Save cleaned images with custom filenames to Pictures/CleanPix directory
- **Share Integration**: Share cleaned images directly from the app
- **Image Validation**: Comprehensive validation for file size (max 25MB) and dimensions (max 8192x8192)
- **Privacy Transparency**: Detailed privacy policy explaining exactly what the app does (and doesn't do)

## Architecture

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Navigation**: Jetpack Navigation Compose
- **Image Handling**: AndroidX ExifInterface, Coil
- **Async Operations**: Kotlin Coroutines and Flow
- **Build System**: Gradle (Kotlin DSL)

### Platform Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Compile SDK**: 36

### Project Structure

```
app/src/main/java/com/harrisonog/cleanpix/
├── MainActivity.kt                    # Entry point
├── ui/
│   ├── MainViewModel.kt               # State management & business logic
│   ├── screens/
│   │   ├── ImageSelectionScreen.kt    # Image picker with onboarding
│   │   ├── ImageMetadataScreen.kt     # Metadata display & processing
│   │   └── PrivacyPolicyScreen.kt     # Privacy policy viewer
│   └── theme/                         # Material3 theming
├── navigation/
│   └── Navigation.kt                  # Navigation graph
└── data/
    └── MetadataStripper.kt            # Core metadata logic
```

### Key Components

**MetadataStripper** (`data/MetadataStripper.kt`)
- Core business logic for metadata operations
- Image validation (MIME type, file size, dimensions)
- EXIF metadata reading and extraction
- Metadata removal by re-encoding images as JPEG
- MediaStore integration for saving images

**MainViewModel** (`ui/MainViewModel.kt`)
- Manages application state using Kotlin Flow
- Coordinates image selection workflow
- Handles metadata operations and error states
- Persists first-run state via SharedPreferences

**Navigation** (`navigation/Navigation.kt`)
- Three-screen flow: Image Selection → Image Metadata → Privacy Policy
- State-driven navigation based on ViewModel

**UI Screens**
- **ImageSelectionScreen**: Photo picker with first-run onboarding overlay
- **ImageMetadataScreen**: Side-by-side display of original and cleaned metadata with action buttons
- **PrivacyPolicyScreen**: Scrollable privacy policy viewer

### Data Flow

1. User selects image from gallery (ImageSelectionScreen)
2. ViewModel receives image URI and reads EXIF metadata
3. Original metadata displayed in ImageMetadataScreen
4. User taps "Remove Metadata" button
5. MetadataStripper creates cleaned copy with metadata removed
6. Cleaned metadata displayed for verification
7. User can save or share the cleaned image

## Privacy & Security

- **No Internet Permission**: App operates entirely offline
- **No Analytics or Tracking**: Zero data collection or transmission
- **Local Processing Only**: All operations happen on-device
- **Minimal Permissions**: Only requests access to read media images
- **Secure File Sharing**: Uses FileProvider for safe file operations
- **Open Source**: Full transparency of what the app does

## Building & Running

### Prerequisites

- Android Studio Ladybug or later
- JDK 11 or later
- Android SDK with API level 36

### Build

```bash
./gradlew assembleDebug
```

### Run Tests

```bash
./gradlew test
```

### Install

```bash
./gradlew installDebug
```

## Supported Image Formats

- JPEG
- PNG

**Limitations:**
- Maximum file size: 25MB
- Maximum dimensions: 8192 x 8192 pixels

## License

[Add your license information here]

## Contributing

[Add contribution guidelines here]
