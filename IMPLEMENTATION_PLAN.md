# PrintMe - Implementation Plan

> ⚠️ **AI-Generated Code Notice**: This project was created fully by AI (GitHub Copilot with Claude). Please review all code before production use.

This document contains the detailed technical implementation plan for the PrintMe Android application.

## Table of Contents

1. [Technical Stack](#technical-stack)
2. [Architecture](#architecture)
3. [Detailed Implementation Plan](#detailed-implementation-plan)
4. [Data Models](#data-models)
5. [UI/UX Design](#uiux-design)
6. [Error Handling](#error-handling)
7. [Edge Cases](#edge-cases)
8. [Testing Strategy](#testing-strategy)
9. [Future Enhancements](#future-enhancements)

---

## Technical Stack

### Native Android (Kotlin) - Minimal Dependencies

| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | Kotlin | Modern, concise, official Android language |
| Min SDK | API 26 (Android 8.0) | Good balance of features and device coverage |
| UI | Jetpack Compose | Modern declarative UI, less boilerplate |
| Image Loading | Coil | Lightweight, Kotlin-first image loader |
| PDF Generation | Android Print Framework | Built-in, no external dependencies |
| Printing | Android PrintManager | Native printing API, zero dependencies |
| Storage | DataStore | Lightweight preferences storage |
| Architecture | MVVM | Simple, well-supported pattern |

### Why Native Android Instead of Cross-Platform?

| Approach | Pros | Cons |
|----------|------|------|
| **Native Android (Chosen)** | Smallest APK, best performance, direct access to Print APIs, no runtime overhead | Android-only |
| Flutter | Cross-platform | Larger APK (~15MB+), runtime overhead |
| React Native | Cross-platform | Larger APK, JS bridge overhead, printing plugins |
| Electron | Desktop focus | Not suitable for Android |

### Dependencies (Minimal)

```kotlin
// build.gradle.kts
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Jetpack Compose (UI)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
// Total: ~6 dependencies (excluding transitive)
// APK size: ~5-8 MB
```

---

## Architecture

### Directory Structure

```
print_me/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/printme/
│   │   │   │   ├── MainActivity.kt           # Single activity entry
│   │   │   │   ├── PrintMeApp.kt             # Application class
│   │   │   │   │
│   │   │   │   ├── ui/                       # UI Layer
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HomeScreen.kt     # Main screen with photo grid
│   │   │   │   │   │   ├── PreviewScreen.kt  # Page preview
│   │   │   │   │   │   └── SettingsScreen.kt # Layout/margin settings
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── PhotoGrid.kt      # Photo thumbnail grid
│   │   │   │   │   │   ├── PhotoItem.kt      # Single photo thumbnail
│   │   │   │   │   │   ├── LayoutSelector.kt # 2/3/4 layout picker
│   │   │   │   │   │   ├── MarginControls.kt # Margin sliders
│   │   │   │   │   │   ├── PagePreview.kt    # Print page preview
│   │   │   │   │   │   └── PageNavigator.kt  # Page navigation
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Colors.kt
│   │   │   │   │
│   │   │   │   ├── viewmodel/                # ViewModels
│   │   │   │   │   ├── PhotoViewModel.kt     # Photo selection state
│   │   │   │   │   ├── LayoutViewModel.kt    # Layout configuration
│   │   │   │   │   └── PrintViewModel.kt     # Print operations
│   │   │   │   │
│   │   │   │   ├── model/                    # Data Models
│   │   │   │   │   ├── Photo.kt
│   │   │   │   │   ├── LayoutConfig.kt
│   │   │   │   │   ├── MarginConfig.kt
│   │   │   │   │   └── PageLayout.kt
│   │   │   │   │
│   │   │   │   ├── service/                  # Business Logic
│   │   │   │   │   ├── PhotoLoader.kt        # Load & validate photos
│   │   │   │   │   ├── LayoutCalculator.kt   # Calculate page layouts
│   │   │   │   │   ├── PageRenderer.kt       # Render pages to bitmap
│   │   │   │   │   └── PrintService.kt       # Android print integration
│   │   │   │   │
│   │   │   │   └── util/                     # Utilities
│   │   │   │       ├── ImageUtils.kt         # Image manipulation
│   │   │   │       ├── Constants.kt          # App constants
│   │   │   │       └── Extensions.kt         # Kotlin extensions
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── dimens.xml
│   │   │   │   └── drawable/
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/                             # Unit tests
│   │
│   └── build.gradle.kts
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Component Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    NavHost (Compose)                       │  │
│  │  ┌─────────────┐ ┌──────────────┐ ┌──────────────────┐    │  │
│  │  │ HomeScreen  │ │PreviewScreen │ │  SettingsScreen  │    │  │
│  │  │             │ │              │ │                  │    │  │
│  │  │ - PhotoGrid │ │ - PageCanvas │ │ - LayoutSelector │    │  │
│  │  │ - Add/Remove│ │ - PageNav    │ │ - MarginControls │    │  │
│  │  │ - Reorder   │ │ - Print btn  │ │ - Paper size     │    │  │
│  │  └──────┬──────┘ └──────┬───────┘ └────────┬─────────┘    │  │
│  │         │               │                  │               │  │
│  └─────────┼───────────────┼──────────────────┼───────────────┘  │
│            │               │                  │                  │
│  ┌─────────▼───────────────▼──────────────────▼───────────────┐  │
│  │                    ViewModels                               │  │
│  │  PhotoViewModel  │  LayoutViewModel  │  PrintViewModel     │  │
│  └─────────┬───────────────┬──────────────────┬───────────────┘  │
│            │               │                  │                  │
│  ┌─────────▼───────────────▼──────────────────▼───────────────┐  │
│  │                     Services                                │  │
│  │  PhotoLoader │ LayoutCalculator │ PageRenderer │ PrintSvc  │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │              Android APIs                                    │  │
│  │  ContentResolver │ PrintManager │ Bitmap/Canvas │ DataStore │  │
│  └─────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

---

## Detailed Implementation Plan

### Phase 1: Project Setup & Core Infrastructure (Week 1)

#### 1.1 Initialize Project
- [ ] Create new Android project with Compose in Android Studio
- [ ] Configure Gradle with minimal dependencies
- [ ] Set up project structure (packages, folders)
- [ ] Configure ProGuard for release builds

#### 1.2 Permissions & Configuration
- [ ] Add READ_MEDIA_IMAGES permission (Android 13+)
- [ ] Add READ_EXTERNAL_STORAGE fallback (Android 12-)
- [ ] Configure AndroidManifest.xml
- [ ] Set up permission request flow

#### 1.3 Base UI Components
- [ ] Create app theme (Material 3)
- [ ] Implement bottom navigation (Home, Preview, Settings)
- [ ] Create reusable Button, Slider components
- [ ] Add loading states and error displays

### Phase 2: Photo Selection & Management (Week 2)

#### 2.1 Photo Picker
- [ ] Implement Android Photo Picker (Android 13+)
- [ ] Fallback to Intent.ACTION_GET_CONTENT (older versions)
- [ ] Support multi-select
- [ ] Handle permission denied gracefully

#### 2.2 Photo Loading
- [ ] Load photos via ContentResolver
- [ ] Generate thumbnails using Coil
- [ ] Handle EXIF orientation
- [ ] Validate image format (JPEG, PNG, WebP)

#### 2.3 Photo Grid UI
- [ ] Create LazyVerticalGrid for photo display
- [ ] Implement photo selection/deselection
- [ ] Add drag-and-drop reordering
- [ ] Implement photo removal with undo (Snackbar)

#### 2.4 Photo Transformations
- [ ] Implement 90° rotation
- [ ] Apply rotation to bitmap before print
- [ ] Store rotation state per photo

### Phase 3: Layout System (Week 3)

#### 3.1 Layout Configuration
- [ ] Create layout selector UI (2, 3, 4 photos)
- [ ] Implement layout preview icons
- [ ] Store layout preference in ViewModel

#### 3.2 Layout Calculations

```kotlin
data class LayoutConfig(
    val photosPerRow: Int,        // 2 or 3
    val rowsPerPage: Int,         // 1 for 2-3 photos, 2 for 4 photos (2x2)
    val gapBetweenPhotos: Float   // mm
)

data class PhotoSlot(
    val x: Float,      // position from left (mm)
    val y: Float,      // position from top (mm)
    val width: Float,  // slot width (mm)
    val height: Float  // slot height (mm)
)
```

- [ ] Calculate slot dimensions based on paper size
- [ ] Handle aspect ratio preservation (fit inside slot)
- [ ] Implement contain fit mode (no cropping)

#### 3.3 Margin System

```kotlin
data class MarginConfig(
    val enabled: Boolean = true,
    val top: Float = 8f,           // mm
    val right: Float = 8f,         // mm
    val bottom: Float = 20f,       // mm (instant camera style)
    val left: Float = 8f,          // mm
    val backgroundColor: Int = Color.WHITE
)

enum class MarginPreset {
    NONE,
    POLAROID,    // top=8, sides=8, bottom=25
    INSTAX,      // top=5, sides=5, bottom=15
    EQUAL,       // all sides equal
    CUSTOM
}
```

- [ ] Create margin control sliders
- [ ] Implement preset buttons (Polaroid, Instax, None)
- [ ] Live preview of margin changes
- [ ] Validate margins fit on paper

### Phase 4: Page Generation & Preview (Week 4)

#### 4.1 Pagination Logic

```kotlin
fun calculatePages(
    photos: List<Photo>,
    layout: LayoutConfig
): List<Page> {
    val photosPerPage = layout.photosPerRow * layout.rowsPerPage
    return photos.chunked(photosPerPage).mapIndexed { index, pagePhotos ->
        Page(pageNumber = index + 1, photos = pagePhotos)
    }
}
```

- [ ] Calculate total pages needed
- [ ] Handle partial last page (empty slots)
- [ ] Support paper sizes: A4, Letter, 4x6, 5x7

#### 4.2 Page Preview Component
- [ ] Create Canvas-based page renderer
- [ ] Implement pinch-to-zoom
- [ ] Add swipe page navigation
- [ ] Show page count indicator (1/3, 2/3, etc.)

#### 4.3 Page Rendering
- [ ] Render photos to Bitmap with margins
- [ ] Apply layout calculations
- [ ] Scale for preview (low-res) vs print (high-res)
- [ ] Show DPI warnings for low-res photos

### Phase 5: Print Integration (Week 5)

#### 5.1 Android Print Framework
- [ ] Implement PrintDocumentAdapter
- [ ] Generate print-ready bitmaps
- [ ] Handle onLayout() and onWrite() callbacks
- [ ] Support multi-page printing

#### 5.2 Print Service Implementation

```kotlin
class PhotoPrintAdapter(
    private val context: Context,
    private val pages: List<PageBitmap>
) : PrintDocumentAdapter() {
    
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        // Calculate pages based on print attributes
    }
    
    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        // Write pages to PDF output
    }
}
```

- [ ] Handle print job lifecycle
- [ ] Support page range selection
- [ ] Handle cancellation properly

#### 5.3 Print Execution
- [ ] Open Android print dialog via PrintManager
- [ ] Let user select printer, copies, paper size
- [ ] Handle print completion/error callbacks

### Phase 6: Settings & Polish (Week 6)

#### 6.1 User Preferences (DataStore)
- [ ] Save default layout preference
- [ ] Save default margin settings
- [ ] Save last used paper size

#### 6.2 Storage Implementation

```kotlin
val Context.dataStore by preferencesDataStore(name = "settings")

object PreferenceKeys {
    val LAYOUT_TYPE = intPreferencesKey("layout_type")
    val MARGIN_TOP = floatPreferencesKey("margin_top")
    val MARGIN_BOTTOM = floatPreferencesKey("margin_bottom")
    val PAPER_SIZE = stringPreferencesKey("paper_size")
}
```

- [ ] Implement settings read/write
- [ ] Apply saved settings on app start

#### 6.3 UI Polish
- [ ] Add dark mode support (follow system)
- [ ] Add haptic feedback on actions
- [ ] Implement loading indicators
- [ ] Add error Snackbars with retry actions

---

## Data Models

### Photo Model

```kotlin
data class Photo(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,                           // Content URI
    val filename: String,                   // Display name
    val mimeType: String,                   // image/jpeg, image/png, etc.
    val width: Int,                         // pixels
    val height: Int,                        // pixels
    val fileSize: Long,                     // bytes
    val orientation: Int = 0,               // EXIF orientation (0, 90, 180, 270)
    val userRotation: Int = 0,              // User-applied rotation
    val status: PhotoStatus = PhotoStatus.READY
)

enum class PhotoStatus {
    LOADING,
    READY,
    ERROR
}

// Supported formats
val SUPPORTED_MIME_TYPES = listOf(
    "image/jpeg",
    "image/png",
    "image/webp"
)
```

### Layout Model

```kotlin
data class LayoutConfig(
    val type: LayoutType = LayoutType.GRID_2X2,
    val photosPerRow: Int = 2,
    val rowsPerPage: Int = 2,
    val photoGap: Float = 4f               // mm between photos
)

enum class LayoutType {
    HORIZONTAL_2,    // 2 photos side by side, 1 row
    HORIZONTAL_3,    // 3 photos side by side, 1 row
    GRID_2X2         // 2x2 grid, 4 photos
}

data class MarginConfig(
    val enabled: Boolean = true,
    val preset: MarginPreset = MarginPreset.POLAROID,
    val top: Float = 8f,                   // mm
    val right: Float = 8f,                 // mm
    val bottom: Float = 20f,               // mm
    val left: Float = 8f,                  // mm
    val backgroundColor: Int = android.graphics.Color.WHITE
)

enum class MarginPreset {
    NONE,
    POLAROID,
    INSTAX,
    EQUAL,
    CUSTOM
}
```

### Page Model

```kotlin
data class Page(
    val id: String = UUID.randomUUID().toString(),
    val pageNumber: Int,
    val photos: List<Photo>,
    val slots: List<PhotoSlot> = emptyList()
)

data class PhotoSlot(
    val index: Int,
    val x: Float,                          // mm from left
    val y: Float,                          // mm from top
    val width: Float,                      // mm
    val height: Float                      // mm
)

data class PageBitmap(
    val pageNumber: Int,
    val bitmap: Bitmap
)
```

### Paper Size Model

```kotlin
data class PaperSize(
    val name: String,
    val widthMm: Float,
    val heightMm: Float
)

object PaperSizes {
    val A4 = PaperSize("A4", 210f, 297f)
    val LETTER = PaperSize("Letter", 215.9f, 279.4f)
    val PHOTO_4X6 = PaperSize("4×6\"", 101.6f, 152.4f)
    val PHOTO_5X7 = PaperSize("5×7\"", 127f, 177.8f)
    val A5 = PaperSize("A5", 148f, 210f)
    
    val ALL = listOf(A4, LETTER, PHOTO_4X6, PHOTO_5X7, A5)
}
```

---

## UI/UX Design

### Main Screen Layout (Compose)

```
┌────────────────────────────────────────────┐
│  ▼ Status Bar                              │
├────────────────────────────────────────────┤
│  PrintMe                           [⚙️]    │  ← TopAppBar
├────────────────────────────────────────────┤
│                                            │
│  Layout: [2] [3] [4]     Paper: [A4 ▼]    │  ← Quick settings
│                                            │
├────────────────────────────────────────────┤
│                                            │
│  ┌────────────────────────────────────┐   │
│  │                                    │   │
│  │         PAGE PREVIEW               │   │
│  │                                    │   │
│  │    ┌─────────┐  ┌─────────┐       │   │
│  │    │ Photo 1 │  │ Photo 2 │       │   │
│  │    └─────────┘  └─────────┘       │   │
│  │    ┌─────────┐  ┌─────────┐       │   │
│  │    │ Photo 3 │  │ Photo 4 │       │   │
│  │    └─────────┘  └─────────┘       │   │
│  │                                    │   │
│  └────────────────────────────────────┘   │
│                                            │
│            ◄  1 / 2  ►                    │  ← Page nav
│                                            │
├────────────────────────────────────────────┤
│                                            │
│  Selected Photos (8)              [Clear]  │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ... │  ← Horizontal scroll
│  │ 1 │ │ 2 │ │ 3 │ │ 4 │ │ 5 │ │ 6 │     │
│  └───┘ └───┘ └───┘ └───┘ └───┘ └───┘     │
│                                            │
├────────────────────────────────────────────┤
│                                            │
│  [➕ Add Photos]              [🖨️ Print]   │  ← Action buttons
│                                            │
├────────────────────────────────────────────┤
│  [🏠]        [📄]        [⚙️]              │  ← Bottom nav
│  Home      Preview     Settings            │
└────────────────────────────────────────────┘
```

### Settings Screen

```
┌────────────────────────────────────────────┐
│  ← Settings                                │
├────────────────────────────────────────────┤
│                                            │
│  LAYOUT                                    │
│  ────────────────────────────────────────  │
│  Photos per page                           │
│  ○ 2 side-by-side                          │
│  ○ 3 side-by-side                          │
│  ● 4 grid (2×2)                            │
│                                            │
│  MARGINS                                   │
│  ────────────────────────────────────────  │
│  ☑ Enable margins                          │
│                                            │
│  Preset: [Polaroid ▼]                      │
│                                            │
│  Top:      [=====●=======] 8 mm            │
│  Sides:    [=====●=======] 8 mm            │
│  Bottom:   [==========●==] 20 mm           │
│                                            │
│  PAPER                                     │
│  ────────────────────────────────────────  │
│  Size: [A4 ▼]                              │
│                                            │
│  ABOUT                                     │
│  ────────────────────────────────────────  │
│  Version 1.0.0                             │
│  Created with AI assistance                │
│                                            │
└────────────────────────────────────────────┘
```

### Photo Picker Bottom Sheet

```
┌────────────────────────────────────────────┐
│  ═══════════════                           │  ← Drag handle
│                                            │
│  Select Photos                    [Done]   │
│                                            │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐  │
│  │  ✓  │ │     │ │  ✓  │ │     │ │     │  │
│  │     │ │     │ │     │ │     │ │     │  │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘  │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐  │
│  │     │ │  ✓  │ │     │ │     │ │     │  │
│  │     │ │     │ │     │ │     │ │     │  │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘  │
│                    ...                     │
└────────────────────────────────────────────┘
```

---

## Error Handling

### Error Categories

| Category | Examples | Handling Strategy |
|----------|----------|-------------------|
| Permission Errors | Storage access denied | Show rationale, request again |
| File Errors | Corrupted image, unsupported format | Show toast, skip file |
| Memory Errors | Too many large photos | Warn user, limit selection |
| Print Errors | No printer, job failed | Show Snackbar with retry |

### Error Handling Implementation

```kotlin
sealed class AppError(
    val message: String,
    val userMessage: String,
    val recoverable: Boolean = true
) {
    // Permission Errors
    data class PermissionDenied(val permission: String) : AppError(
        message = "Permission denied: $permission",
        userMessage = "Storage access is required to select photos",
        recoverable = true
    )
    
    // File Errors
    data class ImageLoadFailed(val uri: Uri, val cause: Throwable?) : AppError(
        message = "Failed to load image: $uri",
        userMessage = "Could not load this photo. It may be corrupted.",
        recoverable = false
    )
    
    data class UnsupportedFormat(val mimeType: String) : AppError(
        message = "Unsupported format: $mimeType",
        userMessage = "This image format is not supported",
        recoverable = false
    )
    
    // Print Errors
    object NoPrintersAvailable : AppError(
        message = "No printers available",
        userMessage = "No printers found. Please set up a printer.",
        recoverable = true
    )
    
    data class PrintJobFailed(val cause: Throwable?) : AppError(
        message = "Print job failed",
        userMessage = "Printing failed. Please try again.",
        recoverable = true
    )
    
    // Memory Errors
    object OutOfMemory : AppError(
        message = "Out of memory",
        userMessage = "Too many photos selected. Please select fewer photos.",
        recoverable = true
    )
}
```

### User-Facing Error UI

```kotlin
@Composable
fun ErrorSnackbar(
    error: AppError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Snackbar(
        action = {
            if (error.recoverable && onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        },
        dismissAction = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Dismiss")
            }
        }
    ) {
        Text(error.userMessage)
    }
}
```

---

## Edge Cases

### Photo Selection Edge Cases

| Edge Case | Detection | Handling |
|-----------|-----------|----------|
| 0 photos selected | Empty list | Disable preview/print, show prompt |
| 100+ photos selected | List size check | Warn about memory, suggest batching |
| Duplicate photo selected | URI comparison | Skip duplicate silently |
| Photo deleted after selection | ContentResolver query fails | Remove from list, show toast |
| Mixed orientations | Dimension check | Handle per-photo rotation |
| Very large files (20MB+) | File size check | Load downscaled version |
| HEIC format (no support) | MIME type check | Show "format not supported" toast |

### Layout Edge Cases

| Edge Case | Detection | Handling |
|-----------|-----------|----------|
| Photos don't fill last page | Modulo calculation | Show empty slots with placeholder |
| Extreme aspect ratios (panorama) | Ratio > 2.5:1 | Fit with letterboxing |
| Portrait + Landscape mix | Orientation per photo | Keep original, user can rotate |
| Margins exceed slot size | Margin sum check | Cap margins at 50% of slot |
| Paper smaller than min margins | Size validation | Show warning, suggest larger paper |

### Print Edge Cases

| Edge Case | Detection | Handling |
|-----------|-----------|----------|
| No printers configured | Empty printer list from system | Show setup instructions dialog |
| Print job cancelled by user | PrintJob state callback | Silent dismiss |
| Print during app close | Lifecycle awareness | Complete in-progress job |
| Out of memory during render | Try-catch OOM | Reduce bitmap quality, retry |

### Android-Specific Edge Cases

| Edge Case | Detection | Handling |
|-----------|-----------|----------|
| Permission permanently denied | shouldShowRationale = false | Open app settings |
| Android 13+ Photo Picker | SDK version check | Use new picker vs legacy intent |
| Low memory device | ActivityManager.isLowRamDevice | Use smaller thumbnails |
| App killed in background | ViewModel SavedState | Restore photo URIs |

### Memory Management

```kotlin
object MemoryConfig {
    const val MAX_PHOTOS = 50                    // Soft limit
    const val MAX_BITMAP_SIZE_MB = 10            // Per image
    const val THUMBNAIL_SIZE_PX = 256            // For grid display
    const val PREVIEW_QUALITY = 0.7f             // JPEG quality for preview
    const val PRINT_QUALITY = 1.0f               // Full quality for print
}

fun checkMemoryBeforeLoad(photoCount: Int): MemoryCheckResult {
    val runtime = Runtime.getRuntime()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
    val maxMemory = runtime.maxMemory()
    val availablePercent = (maxMemory - usedMemory).toFloat() / maxMemory
    
    return when {
        photoCount > MemoryConfig.MAX_PHOTOS -> 
            MemoryCheckResult.Warning("Consider selecting fewer photos")
        availablePercent < 0.2f -> 
            MemoryCheckResult.Critical("Low memory - select fewer photos")
        else -> 
            MemoryCheckResult.Ok
    }
}
```

---

## Testing Strategy

### Unit Tests (JUnit + MockK)

```kotlin
// Example test cases for layout calculator
class LayoutCalculatorTest {

    @Test
    fun `calculateSlots creates 2 equal slots for HORIZONTAL_2 layout`() {
        val slots = LayoutCalculator.calculateSlots(
            layout = LayoutType.HORIZONTAL_2,
            paperSize = PaperSizes.A4,
            margins = MarginConfig()
        )
        
        assertEquals(2, slots.size)
        assertEquals(slots[0].width, slots[1].width, 0.01f)
    }

    @Test
    fun `calculateSlots creates 4 slots in 2x2 grid for GRID_2X2 layout`() {
        val slots = LayoutCalculator.calculateSlots(
            layout = LayoutType.GRID_2X2,
            paperSize = PaperSizes.A4,
            margins = MarginConfig()
        )
        
        assertEquals(4, slots.size)
    }

    @Test
    fun `calculateSlots accounts for margins in slot dimensions`() {
        val margins = MarginConfig(top = 10f, bottom = 20f, left = 10f, right = 10f)
        val slots = LayoutCalculator.calculateSlots(
            layout = LayoutType.HORIZONTAL_2,
            paperSize = PaperSizes.A4,
            margins = margins
        )
        
        // Verify slots fit within printable area
        slots.forEach { slot ->
            assertTrue(slot.x >= margins.left)
            assertTrue(slot.y >= margins.top)
        }
    }

    @Test
    fun `calculatePages creates correct number of pages`() {
        val photos = List(10) { mockPhoto() }
        val pages = LayoutCalculator.calculatePages(
            photos = photos,
            layout = LayoutType.GRID_2X2  // 4 per page
        )
        
        assertEquals(3, pages.size)  // 4+4+2
    }
}
```

### UI Tests (Compose Testing)

```kotlin
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `add photos button is visible when no photos selected`() {
        composeTestRule.setContent {
            HomeScreen(photos = emptyList())
        }
        
        composeTestRule
            .onNodeWithText("Add Photos")
            .assertIsDisplayed()
    }

    @Test
    fun `print button is disabled when no photos selected`() {
        composeTestRule.setContent {
            HomeScreen(photos = emptyList())
        }
        
        composeTestRule
            .onNodeWithText("Print")
            .assertIsNotEnabled()
    }

    @Test
    fun `photo count is displayed correctly`() {
        val photos = List(5) { mockPhoto() }
        composeTestRule.setContent {
            HomeScreen(photos = photos)
        }
        
        composeTestRule
            .onNodeWithText("Selected Photos (5)")
            .assertIsDisplayed()
    }
}
```

### Integration Tests

- [ ] Photo picker → Photo loading → Grid display
- [ ] Layout change → Page recalculation → Preview update
- [ ] Print button → PDF generation → Android print dialog
- [ ] Settings change → DataStore → App restart → Settings restored

### Manual Testing Checklist

- [ ] Test with 1, 2, 3, 4, 5, 10, 50 photos
- [ ] Test all layout combinations (2, 3, 4)
- [ ] Test with corrupted image files
- [ ] Test with JPEG, PNG, WebP formats
- [ ] Test print to PDF (virtual printer)
- [ ] Test print to physical printer
- [ ] Test on Android 8, 10, 12, 13, 14
- [ ] Test on low-memory devices
- [ ] Test with dark mode enabled
- [ ] Test screen rotation during print

---

## Future Enhancements

### Version 1.1
- [ ] Photo crop/edit before printing
- [ ] Custom text captions on margins
- [ ] Date stamp on photos
- [ ] Share print-ready images

### Version 1.2
- [ ] Multiple margin presets library
- [ ] Photo filters (B&W, Sepia)
- [ ] Border/frame styles
- [ ] Export to PDF file

### Version 2.0
- [ ] Cloud photo import (Google Photos)
- [ ] Wi-Fi direct printing
- [ ] Template saving
- [ ] Batch processing

---

## Development Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

# Install on device
./gradlew installDebug
```

---

## Required Permissions

```xml
<!-- AndroidManifest.xml -->

<!-- For Android 13+ (API 33+) -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- For Android 12 and below -->
<uses-permission 
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

---

*Last updated: December 2024*
