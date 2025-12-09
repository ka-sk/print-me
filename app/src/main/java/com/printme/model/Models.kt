package com.printme.model

import android.net.Uri

/**
 * Represents a selected photo with its metadata
 */
data class Photo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val rotation: Int = 0 // Rotation in degrees (0, 90, 180, 270)
)

/**
 * Layout configuration for photos per page
 */
enum class LayoutType(val photosPerPage: Int, val displayName: String) {
    TWO_PER_PAGE(2, "2 per page"),
    THREE_PER_PAGE(3, "3 per page"),
    FOUR_PER_PAGE(4, "4 per page")
}

/**
 * How to handle pages with fewer photos than the layout allows
 */
enum class IncompletePageMode(val displayName: String, val description: String) {
    LEAVE_BLANK("Leave blank", "Empty spaces remain where photos would be"),
    FILL_LAYOUT("Adjust layout", "Change layout to fit the number of photos")
}

/**
 * Margin configuration for instant-camera style borders
 * For portrait photos: large margin goes on bottom
 * For landscape photos: large margin goes on left side
 */
data class MarginConfig(
    val topMm: Float = 8f,
    val bottomMm: Float = 25f,
    val leftMm: Float = 8f,
    val rightMm: Float = 8f
) {
    companion object {
        val DEFAULT = MarginConfig()
        val MINIMAL = MarginConfig(4f, 4f, 4f, 4f)
        val INSTANT_CAMERA = MarginConfig(8f, 25f, 8f, 8f)
    }
    
    /**
     * Get margins adjusted for photo orientation
     * For landscape photos, the larger margin should be on the left side
     * For portrait photos, the larger margin should be on the bottom
     */
    fun getOrientedMargins(photoWidth: Float, photoHeight: Float): OrientedMargins {
        val largeMargin = maxOf(topMm, bottomMm)
        val smallMargin = minOf(topMm, bottomMm)
        val sideMargin = leftMm
        
        return if (photoWidth > photoHeight) {
            // Landscape photo: large margin on left
            OrientedMargins(
                topMm = sideMargin,
                bottomMm = sideMargin,
                leftMm = largeMargin,
                rightMm = smallMargin
            )
        } else {
            // Portrait photo: large margin on bottom
            OrientedMargins(
                topMm = smallMargin,
                bottomMm = largeMargin,
                leftMm = sideMargin,
                rightMm = sideMargin
            )
        }
    }
}

/**
 * Oriented margins after applying orientation logic
 */
data class OrientedMargins(
    val topMm: Float,
    val bottomMm: Float,
    val leftMm: Float,
    val rightMm: Float
)

/**
 * Standard paper sizes with dimensions in mm
 */
enum class PaperSize(val widthMm: Float, val heightMm: Float, val displayName: String) {
    A4(210f, 297f, "A4"),
    A5(148f, 210f, "A5"),
    LETTER(215.9f, 279.4f, "Letter"),
    PHOTO_4X6(101.6f, 152.4f, "4×6\"")
}

/**
 * Represents a single page with arranged photos
 */
data class Page(
    val pageNumber: Int,
    val photos: List<Photo>,
    val layout: LayoutType
)

/**
 * Position and size of a photo on the page
 */
data class PhotoPlacement(
    val photo: Photo,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val margins: OrientedMargins
)

/**
 * Complete print job configuration
 */
data class PrintJob(
    val pages: List<Page>,
    val paperSize: PaperSize,
    val marginConfig: MarginConfig,
    val copies: Int = 1
)
