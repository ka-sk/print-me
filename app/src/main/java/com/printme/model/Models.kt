package com.printme.model

import android.net.Uri

/**
 * Represents a selected photo with its metadata
 */
data class Photo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long = System.currentTimeMillis()
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
 * Margin configuration for instant-camera style borders
 */
data class MarginConfig(
    val topMm: Float = 8f,
    val bottomMm: Float = 25f, // Larger for instant camera style
    val leftMm: Float = 8f,
    val rightMm: Float = 8f
) {
    companion object {
        val DEFAULT = MarginConfig()
        val MINIMAL = MarginConfig(4f, 4f, 4f, 4f)
        val INSTANT_CAMERA = MarginConfig(8f, 25f, 8f, 8f)
    }
}

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
    val margins: MarginConfig
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
