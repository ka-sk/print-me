package com.printme.service

import com.printme.model.LayoutType
import com.printme.model.MarginConfig
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
    fun createPages(photos: List<Photo>, layoutType: LayoutType): List<Page> {
        if (photos.isEmpty()) return emptyList()

        return photos.chunked(layoutType.photosPerPage).mapIndexed { index, pagePhotos ->
            Page(
                pageNumber = index + 1,
                photos = pagePhotos,
                layout = layoutType
            )
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
        margins: MarginConfig
    ): List<PhotoPlacement> {
        // Two photos stacked vertically
        val photoTotalHeight = (pageHeight - 30f) / 2 // 30mm spacing between photos
        val photoTotalWidth = pageWidth - 20f // 10mm margins on each side

        // Photo dimensions inside the frame (excluding margins)
        val photoWidth = photoTotalWidth - margins.leftMm - margins.rightMm
        val photoHeight = photoTotalHeight - margins.topMm - margins.bottomMm

        val startX = 10f // 10mm from left edge

        return photos.mapIndexed { index, photo ->
            val y = 10f + index * (photoTotalHeight + 10f) // 10mm from top, 10mm gap between
            PhotoPlacement(
                photo = photo,
                x = startX,
                y = y,
                width = photoWidth,
                height = photoHeight,
                margins = margins
            )
        }
    }

    private fun calculateThreePerPage(
        photos: List<Photo>,
        pageWidth: Float,
        pageHeight: Float,
        margins: MarginConfig
    ): List<PhotoPlacement> {
        // Three photos: one on top, two on bottom
        val topPhotoWidth = pageWidth - 20f
        val topPhotoHeight = (pageHeight - 30f) * 0.5f

        val bottomPhotoWidth = (pageWidth - 30f) / 2
        val bottomPhotoHeight = (pageHeight - 30f) * 0.45f

        val placements = mutableListOf<PhotoPlacement>()

        // First photo on top (centered)
        if (photos.isNotEmpty()) {
            placements.add(
                PhotoPlacement(
                    photo = photos[0],
                    x = 10f,
                    y = 10f,
                    width = topPhotoWidth - margins.leftMm - margins.rightMm,
                    height = topPhotoHeight - margins.topMm - margins.bottomMm,
                    margins = margins
                )
            )
        }

        // Bottom row photos
        photos.drop(1).forEachIndexed { index, photo ->
            placements.add(
                PhotoPlacement(
                    photo = photo,
                    x = 10f + index * (bottomPhotoWidth + 10f),
                    y = topPhotoHeight + 20f,
                    width = bottomPhotoWidth - margins.leftMm - margins.rightMm,
                    height = bottomPhotoHeight - margins.topMm - margins.bottomMm,
                    margins = margins
                )
            )
        }

        return placements
    }

    private fun calculateFourPerPage(
        photos: List<Photo>,
        pageWidth: Float,
        pageHeight: Float,
        margins: MarginConfig
    ): List<PhotoPlacement> {
        // Four photos in a 2x2 grid
        val photoTotalWidth = (pageWidth - 30f) / 2 // 10mm margins on sides, 10mm gap in middle
        val photoTotalHeight = (pageHeight - 30f) / 2

        val photoWidth = photoTotalWidth - margins.leftMm - margins.rightMm
        val photoHeight = photoTotalHeight - margins.topMm - margins.bottomMm

        return photos.mapIndexed { index, photo ->
            val col = index % 2
            val row = index / 2
            PhotoPlacement(
                photo = photo,
                x = 10f + col * (photoTotalWidth + 10f),
                y = 10f + row * (photoTotalHeight + 10f),
                width = photoWidth,
                height = photoHeight,
                margins = margins
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
