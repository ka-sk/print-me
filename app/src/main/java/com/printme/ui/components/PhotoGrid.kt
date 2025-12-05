package com.printme.ui.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.printme.model.Photo

/**
 * Grid of selectable photo thumbnails
 */
@Composable
fun PhotoGrid(
    photos: List<Photo>,
    selectedPhotos: List<Photo>,
    onPhotoClick: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = modifier
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoThumbnail(
                photo = photo,
                isSelected = selectedPhotos.contains(photo),
                onClick = { onPhotoClick(photo) },
                modifier = Modifier
            )
        }
    }
}
