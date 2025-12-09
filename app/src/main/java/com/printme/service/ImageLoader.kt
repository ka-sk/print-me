package com.printme.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.min

/**
 * Utility class for loading and processing images
 */
class ImageLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageLoader"
    }
    
    /**
     * Load a bitmap asynchronously, scaled to fit the requested dimensions
     */
    suspend fun loadBitmap(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? = withContext(Dispatchers.IO) {
        loadBitmapSync(uri, reqWidth, reqHeight)
    }
    
    /**
     * Load a bitmap synchronously, scaled to fit the requested dimensions
     */
    fun loadBitmapSync(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            // First, decode bounds only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            
            // Decode the bitmap
            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            // Handle EXIF rotation
            bitmap?.let { bmp ->
                val rotation = getExifRotation(uri)
                if (rotation != 0) {
                    rotateBitmap(bmp, rotation)
                } else {
                    bmp
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from $uri", e)
            null
        }
    }
    
    /**
     * Calculate the sample size for efficient bitmap loading
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Get EXIF rotation for an image
     */
    private fun getExifRotation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EXIF", e)
            0
        }
    }
    
    /**
     * Rotate a bitmap by the given degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }
    
    /**
     * Get the dimensions of an image without loading the full bitmap
     */
    fun getImageDimensions(uri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            // Check for EXIF rotation
            val rotation = getExifRotation(uri)
            val width = options.outWidth
            val height = options.outHeight
            
            if (rotation == 90 || rotation == 270) {
                Pair(height, width)
            } else {
                Pair(width, height)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dimensions for $uri", e)
            null
        }
    }
    
    /**
     * Determine if an image is landscape oriented
     */
    fun isLandscape(uri: Uri): Boolean {
        val dimensions = getImageDimensions(uri) ?: return false
        return dimensions.first > dimensions.second
    }
    
    /**
     * Get the aspect ratio of an image (width / height)
     */
    fun getAspectRatio(uri: Uri): Float? {
        val dimensions = getImageDimensions(uri) ?: return null
        return dimensions.first.toFloat() / dimensions.second.toFloat()
    }
}
