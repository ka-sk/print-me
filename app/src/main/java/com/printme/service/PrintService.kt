package com.printme.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.printme.model.MarginConfig
import com.printme.model.Page
import com.printme.model.PaperSize
import kotlinx.coroutines.runBlocking
import java.io.FileOutputStream

/**
 * Service for handling print operations
 */
class PrintService(
    private val context: Context,
    private val imageLoader: ImageLoader
) {

    /**
     * Starts a print job with the given pages
     */
    fun print(
        pages: List<Page>,
        paperSize: PaperSize,
        marginConfig: MarginConfig,
        jobName: String = "PrintMe Photos"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val printAdapter = PhotoPrintAdapter(
            context = context,
            imageLoader = imageLoader,
            pages = pages,
            paperSize = paperSize,
            marginConfig = marginConfig
        )

        printManager.print(jobName, printAdapter, null)
    }

    /**
     * Custom PrintDocumentAdapter for photo printing
     */
    private class PhotoPrintAdapter(
        private val context: Context,
        private val imageLoader: ImageLoader,
        private val pages: List<Page>,
        private val paperSize: PaperSize,
        private val marginConfig: MarginConfig
    ) : PrintDocumentAdapter() {

        private var pdfDocument: PdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }

            pdfDocument = PdfDocument()

            val info = PrintDocumentInfo.Builder("print_me_photos.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                .setPageCount(pages.size)
                .build()

            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pageRanges: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }

            val pdf = pdfDocument ?: return

            try {
                // DPI for print quality
                val dpi = 300
                val pageWidthPixels = LayoutCalculator.mmToPixels(paperSize.widthMm, dpi).toInt()
                val pageHeightPixels = LayoutCalculator.mmToPixels(paperSize.heightMm, dpi).toInt()

                pages.forEachIndexed { index, page ->
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        pageWidthPixels,
                        pageHeightPixels,
                        index + 1
                    ).create()

                    val pdfPage = pdf.startPage(pageInfo)
                    drawPage(pdfPage.canvas, page, dpi)
                    pdf.finishPage(pdfPage)
                }

                pdf.writeTo(FileOutputStream(destination.fileDescriptor))
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))

            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                pdf.close()
            }
        }

        private fun drawPage(canvas: Canvas, page: Page, dpi: Int) {
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

                // Load and draw photo
                val bitmap = runBlocking {
                    loadBitmap(placement.photo.uri.toString(), photoWidth.toInt(), photoHeight.toInt())
                }

                bitmap?.let {
                    val scaledBitmap = scaleBitmapToFit(it, photoWidth.toInt(), photoHeight.toInt())
                    
                    // Center the bitmap in the available space
                    val offsetX = (photoWidth - scaledBitmap.width) / 2
                    val offsetY = (photoHeight - scaledBitmap.height) / 2
                    
                    canvas.drawBitmap(
                        scaledBitmap,
                        photoX + offsetX,
                        photoY + offsetY,
                        null
                    )
                    
                    if (scaledBitmap != it) {
                        scaledBitmap.recycle()
                    }
                    it.recycle()
                }
            }
        }

        private suspend fun loadBitmap(uri: String, width: Int, height: Int): Bitmap? {
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

        override fun onFinish() {
            pdfDocument?.close()
            pdfDocument = null
        }
    }
}
