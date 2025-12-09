package com.printme.ui.util

import android.graphics.Bitmap
import android.graphics.Matrix
import coil.size.Size
import coil.transform.Transformation

/**
 * Coil transformation to rotate an image
 */
class RotateTransformation(private val degrees: Int) : Transformation {
    
    override val cacheKey: String = "rotate_$degrees"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (degrees % 360 == 0) return input
        
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val output = Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
        
        if (input != output && !input.isRecycled) {
            input.recycle()
        }
        
        return output
    }
}
