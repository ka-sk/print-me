package com.printme.service

import com.printme.model.IncompletePageMode
import com.printme.model.LayoutType
import com.printme.model.MarginConfig
import com.printme.model.OrientedMargins
import com.printme.model.Page
import com.printme.model.PaperSize
import com.printme.model.Photo
import com.printme.model.PhotoPlacement

/**
 * Calculates photo placements on pages based on layout configuration
 */
object LayoutCalculator {

    /**
     * Creates pages from selected photos based on layout type
     */
    fun createPages(
        photos: List<Photo>,
        layoutType: LayoutType,
        incompletePageMode: IncompletePageMode = IncompletePageMode.LEAVE_BLANK
    ): List<Page> {
        if (photos.isEmpty()) return emptyList()

        return when (incompletePageMode) {
            IncompletePageMode.LEAVE_BLANK -> {
                // Standard chunking - may leave blank spaces on last page
                photos.chunked(layoutType.photosPerPage).mapIndexed { index, pagePhotos ->
                    Page(
                        pageNumber = index + 1,
                        photos = pagePhotos,
                        layout = layoutType
                    )
                }
            }
            IncompletePageMode.FILL_LAYOUT -> {
                // Adjust layout for incomplete pages
                val fullPages = photos.chunked(layoutType.photosPerPage)
                fullPages.mapIndexed { index, pagePhotos ->
                    val effectiveLayout = if (pagePhotos.size < layoutType.photosPerPage) {
                        // Find the best fitting layout for remaining photos
                        getBestLayoutForCount(pagePhotos.size)
                    } else {
                        layoutType
                    }
                    Page(
                        pageNumber = index + 1,
                        photos = pagePhotos,
                        layout = effectiveLayout
                    )
                }
            }
        }
    }

    /**
     * Find the best layout for a given number of photos
     */
    private fun getBestLayoutForCount(count: Int): LayoutType {
        return when {
            count <= 2 -> LayoutType.TWO_PER_PAGE
            count == 3 -> LayoutType.THREE_PER_PAGE
            else -> LayoutType.FOUR_PER_PAGE
        }
    }

    /**
     * Calculates placements for photos on a page
     * All dimensions in mm
     */
    fun calculatePlacements(
        page: Page,
        paperSize: PaperSize,
        marginConfig: MarginConfig
    ): List<PhotoPlacement> {
        val printableWidth = paperSize.widthMm
        val printableHeight = paperSize.heightMm

        return when (page.layout) {
            LayoutType.TWO_PER_PAGE -> calculateTwoPerPage(page.photos, printableWidth, printableHeight, marginConfig)
            LayoutType.THREE_PER_PAGE -> calculateThreePerPage(page.photos, printableWidth, printableHeight, marginConfig)
            LayoutType.FOUR_PER_PAGE -> calculateFourPerPage(page.photos, printableWidth, printableHeight, marginConfig)
        }
    }

    private fun calculateTwoPerPage(
        photos: List<Photo>,
        pageWidth: Float,
        pageHeight: Float,
        marginConfig: MarginConfig
    ): List<PhotoPlacement> {
        // Two photos stacked vertically
        val photoTotalHeight = (pageHeight - 30f) / 2 // 30mm spacing between photos
        val photoTotalWidth = pageWidth - 20f // 10mm margins on each side

        val startX = 10f // 10mm from left edge

        return photos.mapIndexed { index, photo ->
            // Get oriented margins based on photo dimensions in the slot
            val orientedMargins = marginConfig.getOrientedMargins(photoTotalWidth, photoTotalHeight)
            
            // Photo dimensions inside the frame (excluding margins)
            val photoWidth = photoTotalWidth - orientedMargins.leftMm - orientedMargins.rightMm
            val photoHeight = photoTotalHeight - orientedMargins.topMm - orientedMargins.bottomMm
            
            val y = 10f + index * (photoTotalHeight + 10f) // 10mm from top, 10mm gap between
            PhotoPlacement(
                photo = photo,
                x = startX,
                y = y,
                width = photoWidth,
                height = photoHeight,
                margins = orientedMargins
            )
        }
    }

    private fun calculateThreePerPage(
        photos: List<Photo>,
        pageWidth: Float,
        pageHeight: Float,
        marginConfig: MarginConfig
    ): List<PhotoPlacement> {
        // Three photos: one on top, two on bottom
        val topPhotoTotalWidth = pageWidth - 20f
        val topPhotoTotalHeight = (pageHeight - 30f) * 0.5f

        val bottomPhotoTotalWidth = (pageWidth - 30f) / 2
        val bottomPhotoTotalHeight = (pageHeight - 30f) * 0.45f

        val placements = mutableListOf<PhotoPlacement>()

        // First photo on top (centered)
        if (photos.isNotEmpty()) {
            val topMargins = marginConfig.getOrientedMargins(topPhotoTotalWidth, topPhotoTotalHeight)
            placements.add(
                PhotoPlacement(
                    photo = photos[0],
                    x = 10f,
                    y = 10f,
                    width = topPhotoTotalWidth - topMargins.leftMm - topMargins.rightMm,
                    height = topPhotoTotalHeight - topMargins.topMm - topMargins.bottomMm,
                    margins = topMargins
                )
            )
        }

        // Bottom row photos
        photos.drop(1).forEachIndexed { index, photo ->
            val bottomMargins = marginConfig.getOrientedMargins(bottomPhotoTotalWidth, bottomPhotoTotalHeight)
            
            // If we have only 2 photos total (1 on top, 1 on bottom), the bottom one should be in the first slot (left)
            // If we have 3 photos total, the bottom ones fill left and right slots
            // The index here is 0 for the 2nd photo (first in bottom row), 1 for the 3rd photo
            
            // We want to place them in fixed slots.
            // Slot 0 (bottom left): x = 10f
            // Slot 1 (bottom right): x = 10f + bottomPhotoTotalWidth + 10f
            
            // Since we are iterating over available photos, we just place them in order.
            // If there is only 1 photo for the bottom row, it goes to the left slot.
            // This is correct behavior for "Leave blank".
            
            placements.add(
                PhotoPlacement(
                    photo = photo,
                    x = 10f + index * (bottomPhotoTotalWidth + 10f),
                    y = topPhotoTotalHeight + 20f,
                    width = bottomPhotoTotalWidth - bottomMargins.leftMm - bottomMargins.rightMm,
                    height = bottomPhotoTotalHeight - bottomMargins.topMm - bottomMargins.bottomMm,
                    margins = bottomMargins
                )
            )
        }

        return placements
    }

    private fun calculateFourPerPage(
        photos: List<Photo>,
        pageWidth: Float,
        pageHeight: Float,
        marginConfig: MarginConfig
    ): List<PhotoPlacement> {
        // Four photos in a 2x2 grid
        val photoTotalWidth = (pageWidth - 30f) / 2 // 10mm margins on sides, 10mm gap in middle
        val photoTotalHeight = (pageHeight - 30f) / 2

        return photos.mapIndexed { index, photo ->
            val orientedMargins = marginConfig.getOrientedMargins(photoTotalWidth, photoTotalHeight)
            
            val photoWidth = photoTotalWidth - orientedMargins.leftMm - orientedMargins.rightMm
            val photoHeight = photoTotalHeight - orientedMargins.topMm - orientedMargins.bottomMm

            val col = index % 2
            val row = index / 2
            PhotoPlacement(
                photo = photo,
                x = 10f + col * (photoTotalWidth + 10f),
                y = 10f + row * (photoTotalHeight + 10f),
                width = photoWidth,
                height = photoHeight,
                margins = orientedMargins
            )
        }
    }

    /**
     * Converts mm to pixels at given DPI
     */
    fun mmToPixels(mm: Float, dpi: Int = 300): Float {
        return mm * dpi / 25.4f
    }

    /**
     * Converts pixels to mm at given DPI
     */
    fun pixelsToMm(pixels: Float, dpi: Int = 300): Float {
        return pixels * 25.4f / dpi
    }
}
