package com.printme.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.printme.model.LayoutType
import com.printme.model.MarginConfig
import com.printme.model.Page
import com.printme.model.PaperSize
import com.printme.model.Photo
import com.printme.service.LayoutCalculator
import com.printme.service.PhotoLoaderService
import com.printme.service.PreferencesService
import com.printme.service.PrintService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the PrintMe app
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val photoLoaderService = PhotoLoaderService(application.contentResolver)
    private val preferencesService = PreferencesService(application)
    private val imageLoader = ImageLoader.Builder(application).build()
    private val printService = PrintService(application, imageLoader)

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
     * Start print job
     */
    fun print() {
        val currentPages = _pages.value
        if (currentPages.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No pages to print")
            return
        }

        printService.print(
            pages = currentPages,
            paperSize = _paperSize.value,
            marginConfig = _marginConfig.value
        )
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
        _pages.value = LayoutCalculator.createPages(photos, layout)
        
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
