package com.printme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.printme.ui.util.RotateTransformation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.printme.model.LayoutType
import com.printme.model.MarginConfig
import com.printme.model.Page
import com.printme.model.PaperSize
import com.printme.model.Photo

/**
 * Preview of a page with photos arranged according to layout
 */
@Composable
fun PagePreview(
    page: Page,
    paperSize: PaperSize,
    marginConfig: MarginConfig,
    photoRotations: Map<Long, Int> = emptyMap(),
    onRotatePhoto: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val aspectRatio = paperSize.widthMm / paperSize.heightMm

    Card(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            when (page.layout) {
                LayoutType.TWO_PER_PAGE -> TwoPhotoLayout(page, marginConfig, photoRotations, onRotatePhoto)
                LayoutType.THREE_PER_PAGE -> ThreePhotoLayout(page, marginConfig, photoRotations, onRotatePhoto)
                LayoutType.FOUR_PER_PAGE -> FourPhotoLayout(page, marginConfig, photoRotations, onRotatePhoto)
            }
        }
    }
}

@Composable
private fun TwoPhotoLayout(
    page: Page,
    marginConfig: MarginConfig,
    photoRotations: Map<Long, Int>,
    onRotatePhoto: ((Long) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        page.photos.forEach { photo ->
            PhotoFrame(
                photo = photo,
                marginConfig = marginConfig,
                rotation = photoRotations[photo.id] ?: 0,
                onRotate = onRotatePhoto?.let { { it(photo.id) } },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThreePhotoLayout(
    page: Page,
    marginConfig: MarginConfig,
    photoRotations: Map<Long, Int>,
    onRotatePhoto: ((Long) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First photo on top (larger)
        if (page.photos.isNotEmpty()) {
            val photo = page.photos[0]
            PhotoFrame(
                photo = photo,
                marginConfig = marginConfig,
                rotation = photoRotations[photo.id] ?: 0,
                onRotate = onRotatePhoto?.let { { it(photo.id) } },
                modifier = Modifier.weight(1.2f)
            )
        }

        // Bottom two photos
        if (page.photos.size > 1) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                page.photos.drop(1).forEach { photo ->
                    PhotoFrame(
                        photo = photo,
                        marginConfig = marginConfig,
                        rotation = photoRotations[photo.id] ?: 0,
                        onRotate = onRotatePhoto?.let { { it(photo.id) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FourPhotoLayout(
    page: Page,
    marginConfig: MarginConfig,
    photoRotations: Map<Long, Int>,
    onRotatePhoto: ((Long) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chunkedPhotos = page.photos.chunked(2)
        chunkedPhotos.forEach { rowPhotos ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPhotos.forEach { photo ->
                    PhotoFrame(
                        photo = photo,
                        marginConfig = marginConfig,
                        rotation = photoRotations[photo.id] ?: 0,
                        onRotate = onRotatePhoto?.let { { it(photo.id) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty spaces
                repeat(2 - rowPhotos.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Photo frame with instant-camera style margins and rotation support
 */
@Composable
fun PhotoFrame(
    photo: Photo,
    marginConfig: MarginConfig,
    rotation: Int = 0,
    onRotate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Scale margins for preview (original is in mm, we show proportionally)
    val topPadding = (marginConfig.topMm / 2).dp
    val bottomPadding = (marginConfig.bottomMm / 2).dp
    val sidePadding = ((marginConfig.leftMm + marginConfig.rightMm) / 4).dp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = sidePadding,
                    end = sidePadding,
                    top = topPadding,
                    bottom = bottomPadding
                )
        ) {
            val context = LocalContext.current
            val imageRequest = ImageRequest.Builder(context)
                .data(photo.uri)
                .transformations(RotateTransformation(rotation))
                .build()

            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
            
            // Rotation button overlay
            if (onRotate != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .clickable { onRotate() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rotate photo",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Legacy photo frame for backwards compatibility
 */
@Composable
fun PhotoFrame(
    photoUri: String,
    marginConfig: MarginConfig,
    modifier: Modifier = Modifier
) {
    // Scale margins for preview (original is in mm, we show proportionally)
    val topPadding = (marginConfig.topMm / 2).dp
    val bottomPadding = (marginConfig.bottomMm / 2).dp
    val sidePadding = ((marginConfig.leftMm + marginConfig.rightMm) / 4).dp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = sidePadding,
                    end = sidePadding,
                    top = topPadding,
                    bottom = bottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
        }
    }
}

/**
 * Page indicator showing current page number
 */
@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Page $currentPage of $totalPages",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
