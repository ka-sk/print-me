package com.printme.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.printme.model.IncompletePageMode
import com.printme.model.LayoutType
import com.printme.model.MarginConfig
import com.printme.model.Page
import com.printme.model.PaperSize
import com.printme.model.Photo
import com.printme.service.LayoutCalculator
import com.printme.service.PhotoLoaderService
import com.printme.service.PreferencesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Main ViewModel for the PrintMe app
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val photoLoaderService = PhotoLoaderService(application.contentResolver)
    private val preferencesService = PreferencesService(application)
    private val imageLoader = ImageLoader.Builder(application).build()

    // UI State
    private val _uiState = MutableStateFlow(PrintMeUiState())
    val uiState: StateFlow<PrintMeUiState> = _uiState.asStateFlow()

    // All photos from gallery
    private val _allPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val allPhotos: StateFlow<List<Photo>> = _allPhotos.asStateFlow()

    // Selected photos
    private val _selectedPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val selectedPhotos: StateFlow<List<Photo>> = _selectedPhotos.asStateFlow()

    // Generated pages
    private val _pages = MutableStateFlow<List<Page>>(emptyList())
    val pages: StateFlow<List<Page>> = _pages.asStateFlow()

    // Current layout type
    private val _layoutType = MutableStateFlow(LayoutType.FOUR_PER_PAGE)
    val layoutType: StateFlow<LayoutType> = _layoutType.asStateFlow()

    // Paper size
    private val _paperSize = MutableStateFlow(PaperSize.A4)
    val paperSize: StateFlow<PaperSize> = _paperSize.asStateFlow()

    // Margin config
    private val _marginConfig = MutableStateFlow(MarginConfig.INSTANT_CAMERA)
    val marginConfig: StateFlow<MarginConfig> = _marginConfig.asStateFlow()

    // Current preview page index
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // Incomplete page handling mode
    private val _incompletePageMode = MutableStateFlow(IncompletePageMode.LEAVE_BLANK)
    val incompletePageMode: StateFlow<IncompletePageMode> = _incompletePageMode.asStateFlow()

    // Photo rotations map (photoId -> degrees)
    private val _photoRotations = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val photoRotations: StateFlow<Map<Long, Int>> = _photoRotations.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _layoutType.value = preferencesService.layoutType.first()
            _paperSize.value = preferencesService.paperSize.first()
            _marginConfig.value = preferencesService.marginConfig.first()
        }
    }

    /**
     * Load photos from the device gallery
     */
    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val photos = photoLoaderService.loadPhotos()
                _allPhotos.value = photos
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load photos: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle photo selection
     */
    fun togglePhotoSelection(photo: Photo) {
        val currentSelection = _selectedPhotos.value.toMutableList()
        if (currentSelection.contains(photo)) {
            currentSelection.remove(photo)
        } else {
            currentSelection.add(photo)
        }
        _selectedPhotos.value = currentSelection
        updatePages()
    }

    /**
     * Rotate a photo by 90 degrees clockwise (by photo ID)
     */
    fun rotatePhoto(photoId: Long) {
        val currentRotations = _photoRotations.value.toMutableMap()
        val currentRotation = currentRotations[photoId] ?: 0
        val newRotation = (currentRotation + 90) % 360
        currentRotations[photoId] = newRotation
        _photoRotations.value = currentRotations
        
        // Also update the Photo object's rotation
        _selectedPhotos.value = _selectedPhotos.value.map {
            if (it.id == photoId) it.copy(rotation = newRotation) else it
        }
        _allPhotos.value = _allPhotos.value.map {
            if (it.id == photoId) it.copy(rotation = newRotation) else it
        }
        
        updatePages()
    }

    /**
     * Rotate a photo by 90 degrees clockwise (by Photo object)
     */
    fun rotatePhoto(photo: Photo) {
        rotatePhoto(photo.id)
    }

    /**
     * Export pages as images for sharing
     */
    fun exportPages() {
        val currentPages = _pages.value
        if (currentPages.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No pages to export")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val pdfUri = generatePdf(currentPages)
                if (pdfUri != null) {
                    sharePdf(pdfUri)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to export pages"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to export: ${e.message}"
                )
            }
        }
    }

    /**
     * Select all photos
     */
    fun selectAllPhotos() {
        _selectedPhotos.value = _allPhotos.value
        updatePages()
    }

    /**
     * Clear photo selection
     */
    fun clearSelection() {
        _selectedPhotos.value = emptyList()
        _pages.value = emptyList()
        _currentPageIndex.value = 0
    }

    /**
     * Update layout type
     */
    fun setLayoutType(layoutType: LayoutType) {
        _layoutType.value = layoutType
        viewModelScope.launch {
            preferencesService.saveLayoutType(layoutType)
        }
        updatePages()
    }

    /**
     * Update paper size
     */
    fun setPaperSize(paperSize: PaperSize) {
        _paperSize.value = paperSize
        viewModelScope.launch {
            preferencesService.savePaperSize(paperSize)
        }
    }

    /**
     * Update margin configuration
     */
    fun setMarginConfig(marginConfig: MarginConfig) {
        _marginConfig.value = marginConfig
        viewModelScope.launch {
            preferencesService.saveMarginConfig(marginConfig)
        }
    }

    /**
     * Update incomplete page handling mode
     */
    fun setIncompletePageMode(mode: IncompletePageMode) {
        _incompletePageMode.value = mode
        updatePages()
    }

    /**
     * Navigate to next preview page
     */
    fun nextPage() {
        if (_currentPageIndex.value < _pages.value.size - 1) {
            _currentPageIndex.value++
        }
    }

    /**
     * Navigate to previous preview page
     */
    fun previousPage() {
        if (_currentPageIndex.value > 0) {
            _currentPageIndex.value--
        }
    }

    /**
     * Go to specific page
     */
    fun goToPage(index: Int) {
        if (index in _pages.value.indices) {
            _currentPageIndex.value = index
        }
    }

    /**
     * Generate PDF and share/print via system share sheet
     */
    fun print() {
        val currentPages = _pages.value
        if (currentPages.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No pages to print")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val pdfUri = generatePdf(currentPages)
                if (pdfUri != null) {
                    sharePdf(pdfUri)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to generate PDF"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to print: ${e.message}"
                )
            }
        }
    }

    private suspend fun generatePdf(pages: List<Page>): Uri? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val pdfDocument = PdfDocument()
        
        try {
            val dpi = 300
            val paperSize = _paperSize.value
            val marginConfig = _marginConfig.value
            
            val pageWidthPixels = LayoutCalculator.mmToPixels(paperSize.widthMm, dpi).toInt()
            val pageHeightPixels = LayoutCalculator.mmToPixels(paperSize.heightMm, dpi).toInt()

            pages.forEachIndexed { index, page ->
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidthPixels,
                    pageHeightPixels,
                    index + 1
                ).create()

                val pdfPage = pdfDocument.startPage(pageInfo)
                drawPage(pdfPage.canvas, page, dpi, paperSize, marginConfig)
                pdfDocument.finishPage(pdfPage)
            }

            // Save PDF to cache directory
            val cacheDir = File(context.cacheDir, "pdfs")
            cacheDir.mkdirs()
            val pdfFile = File(cacheDir, "printme_photos_${System.currentTimeMillis()}.pdf")
            
            FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            // Get URI via FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    private suspend fun drawPage(
        canvas: Canvas,
        page: Page,
        dpi: Int,
        paperSize: PaperSize,
        marginConfig: MarginConfig
    ) {
        val placements = LayoutCalculator.calculatePlacements(page, paperSize, marginConfig)

        // Background paint
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), bgPaint)

        // Border paint for photo frames
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = LayoutCalculator.mmToPixels(0.5f, dpi)
        }

        // Frame background paint (white for instant camera style)
        val framePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        placements.forEach { placement ->
            val margins = placement.margins

            // Calculate frame dimensions (photo + margins)
            val frameX = LayoutCalculator.mmToPixels(placement.x, dpi)
            val frameY = LayoutCalculator.mmToPixels(placement.y, dpi)
            val frameWidth = LayoutCalculator.mmToPixels(
                placement.width + margins.leftMm + margins.rightMm, dpi
            )
            val frameHeight = LayoutCalculator.mmToPixels(
                placement.height + margins.topMm + margins.bottomMm, dpi
            )

            // Draw frame background
            val frameRect = RectF(frameX, frameY, frameX + frameWidth, frameY + frameHeight)
            canvas.drawRect(frameRect, framePaint)
            canvas.drawRect(frameRect, borderPaint)

            // Calculate photo position within frame
            val photoX = frameX + LayoutCalculator.mmToPixels(margins.leftMm, dpi)
            val photoY = frameY + LayoutCalculator.mmToPixels(margins.topMm, dpi)
            val photoWidth = LayoutCalculator.mmToPixels(placement.width, dpi)
            val photoHeight = LayoutCalculator.mmToPixels(placement.height, dpi)

            // Load bitmap
            // If rotation is 90 or 270, we need to swap width and height for loading
            // to ensure we get enough resolution for the rotated image
            val rotation = placement.photo.rotation
            val (reqWidth, reqHeight) = if (rotation == 90 || rotation == 270) {
                photoHeight.toInt() to photoWidth.toInt()
            } else {
                photoWidth.toInt() to photoHeight.toInt()
            }

            val bitmap = loadBitmap(
                placement.photo.uri.toString(),
                reqWidth,
                reqHeight
            )

            bitmap?.let { originalBitmap ->
                // Apply rotation if needed
                val rotatedBitmap = if (rotation != 0) {
                    rotateBitmap(originalBitmap, rotation)
                } else {
                    originalBitmap
                }
                
                val scaledBitmap = scaleBitmapToFit(rotatedBitmap, photoWidth.toInt(), photoHeight.toInt())
                
                // Center the bitmap in the available space
                val offsetX = (photoWidth - scaledBitmap.width) / 2
                val offsetY = (photoHeight - scaledBitmap.height) / 2
                
                canvas.drawBitmap(
                    scaledBitmap,
                    photoX + offsetX,
                    photoY + offsetY,
                    null
                )
                
                if (scaledBitmap != rotatedBitmap) {
                    scaledBitmap.recycle()
                }
                if (rotatedBitmap != originalBitmap) {
                    rotatedBitmap.recycle()
                }
                originalBitmap.recycle()
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private suspend fun loadBitmap(uri: String, width: Int, height: Int): Bitmap? {
        val context = getApplication<Application>()
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(width, height)
            .build()

        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> result.drawable.toBitmap()
            else -> null
        }
    }

    private fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val widthRatio = maxWidth.toFloat() / bitmap.width
        val heightRatio = maxHeight.toFloat() / bitmap.height
        val ratio = minOf(widthRatio, heightRatio)

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        return if (newWidth == bitmap.width && newHeight == bitmap.height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }

    private fun sharePdf(pdfUri: Uri) {
        val context = getApplication<Application>()
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Print or Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun updatePages() {
        val photos = _selectedPhotos.value
        val layout = _layoutType.value
        val incompleteMode = _incompletePageMode.value
        _pages.value = LayoutCalculator.createPages(photos, layout, incompleteMode)
        
        // Reset page index if it's out of bounds
        if (_currentPageIndex.value >= _pages.value.size) {
            _currentPageIndex.value = maxOf(0, _pages.value.size - 1)
        }
    }
}

/**
 * UI State for the main screen
 */
data class PrintMeUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
