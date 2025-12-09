package com.printme.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import com.printme.model.MarginConfig
import com.printme.model.Page
import com.printme.model.PaperSize
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Service for printing photo pages via Android Print Framework
 */
class PrintService(private val context: Context) {
    
    companion object {
        private const val TAG = "PrintService"
        private const val POINTS_PER_INCH = 72
    }
    
    private val imageLoader = ImageLoader(context)
    
    /**
     * Creates a print job for the given pages
     */
    fun print(
        pages: List<Page>,
        paperSize: PaperSize,
        marginConfig: MarginConfig,
        photoUris: Map<Long, Uri>,
        photoRotations: Map<Long, Int>,
        jobName: String = "Photo Print"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        
        val printAdapter = PhotoPrintAdapter(
            context = context,
            pages = pages,
            paperSize = paperSize,
            marginConfig = marginConfig,
            photoUris = photoUris,
            photoRotations = photoRotations,
            imageLoader = imageLoader
        )
        
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        
        printManager.print(jobName, printAdapter, attributes)
    }
    
    /**
     * Custom PrintDocumentAdapter for photo pages
     */
    private class PhotoPrintAdapter(
        private val context: Context,
        private val pages: List<Page>,
        private val paperSize: PaperSize,
        private val marginConfig: MarginConfig,
        private val photoUris: Map<Long, Uri>,
        private val photoRotations: Map<Long, Int>,
        private val imageLoader: ImageLoader
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
            
            val info = PrintDocumentInfo.Builder("photo_print.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
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
            pdfDocument = PdfDocument()
            
            try {
                val pageWidthPt = (paperSize.widthMm / 25.4f * POINTS_PER_INCH).roundToInt()
                val pageHeightPt = (paperSize.heightMm / 25.4f * POINTS_PER_INCH).roundToInt()
                
                for ((index, page) in pages.withIndex()) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        pdfDocument?.close()
                        return
                    }
                    
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, index).create()
                    val pdfPage = pdfDocument!!.startPage(pageInfo)
                    
                    renderPageToCanvas(pdfPage.canvas, page, pageWidthPt, pageHeightPt)
                    
                    pdfDocument!!.finishPage(pdfPage)
                }
                
                pdfDocument!!.writeTo(FileOutputStream(destination.fileDescriptor))
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                
            } catch (e: Exception) {
                Log.e(TAG, "Error writing PDF", e)
                callback.onWriteFailed(e.message)
            } finally {
                pdfDocument?.close()
            }
        }
        
        @Suppress("UNUSED_PARAMETER")
        private fun renderPageToCanvas(canvas: Canvas, page: Page, pageWidthPt: Int, pageHeightPt: Int) {
            // Fill with white
            canvas.drawColor(android.graphics.Color.WHITE)
            
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            
            // Calculate placements using the layout calculator
            val placements = LayoutCalculator.calculatePlacements(page, paperSize, marginConfig)
            
            for (placement in placements) {
                val photoUri = photoUris[placement.photo.id] ?: continue
                val rotation = photoRotations[placement.photo.id] ?: 0
                
                try {
                    // Convert mm to points
                    val mmToPt = POINTS_PER_INCH / 25.4f
                    val x = (placement.x * mmToPt).roundToInt()
                    val y = (placement.y * mmToPt).roundToInt()
                    val width = (placement.width * mmToPt).roundToInt()
                    val height = (placement.height * mmToPt).roundToInt()
                    
                    val photoBitmap = imageLoader.loadBitmapSync(photoUri, width * 4, height * 4)
                    
                    if (photoBitmap != null) {
                        // Apply rotation
                        val rotatedBitmap = if (rotation != 0) {
                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                            Bitmap.createBitmap(photoBitmap, 0, 0, photoBitmap.width, photoBitmap.height, matrix, true)
                        } else {
                            photoBitmap
                        }
                        
                        // Scale to fit
                        val widthRatio = width.toFloat() / rotatedBitmap.width.toFloat()
                        val heightRatio = height.toFloat() / rotatedBitmap.height.toFloat()
                        val scale = min(widthRatio, heightRatio)
                        
                        val scaledWidth = (rotatedBitmap.width * scale).roundToInt()
                        val scaledHeight = (rotatedBitmap.height * scale).roundToInt()
                        val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, scaledWidth, scaledHeight, true)
                        
                        val offsetX = x + (width - scaledWidth) / 2
                        val offsetY = y + (height - scaledHeight) / 2
                        
                        canvas.drawBitmap(scaledBitmap, offsetX.toFloat(), offsetY.toFloat(), paint)
                        
                        // Cleanup
                        if (rotatedBitmap != photoBitmap) rotatedBitmap.recycle()
                        if (scaledBitmap != rotatedBitmap) scaledBitmap.recycle()
                        photoBitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rendering photo", e)
                }
            }
        }
    }
}
