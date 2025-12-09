package com.printme.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.printme.model.IncompletePageMode
import com.printme.model.LayoutType
import com.printme.model.MarginConfig
import com.printme.model.PaperSize

/**
 * Layout type selector with radio buttons
 */
@Composable
fun LayoutSelector(
    selectedLayout: LayoutType,
    onLayoutSelected: (LayoutType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        Text(
            text = "Photos per page",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LayoutType.entries.forEach { layout ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedLayout == layout,
                        onClick = { onLayoutSelected(layout) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedLayout == layout,
                    onClick = null
                )
                Text(
                    text = layout.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Paper size selector with radio buttons
 */
@Composable
fun PaperSizeSelector(
    selectedSize: PaperSize,
    onSizeSelected: (PaperSize) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        Text(
            text = "Paper size",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        PaperSize.entries.forEach { size ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedSize == size,
                        onClick = { onSizeSelected(size) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSize == size,
                    onClick = null
                )
                Text(
                    text = size.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Margin configuration with sliders
 */
@Composable
fun MarginConfigurator(
    marginConfig: MarginConfig,
    onMarginChanged: (MarginConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var topMargin by remember(marginConfig) { mutableFloatStateOf(marginConfig.topMm) }
    var bottomMargin by remember(marginConfig) { mutableFloatStateOf(marginConfig.bottomMm) }
    var sideMargin by remember(marginConfig) { mutableFloatStateOf(marginConfig.leftMm) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Margins (mm)",
            style = MaterialTheme.typography.titleSmall
        )

        // Top margin
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Top", style = MaterialTheme.typography.bodyMedium)
                Text("${topMargin.toInt()} mm", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = topMargin,
                onValueChange = { topMargin = it },
                onValueChangeFinished = {
                    onMarginChanged(marginConfig.copy(topMm = topMargin))
                },
                valueRange = 2f..30f
            )
        }

        // Bottom margin
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bottom", style = MaterialTheme.typography.bodyMedium)
                Text("${bottomMargin.toInt()} mm", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = bottomMargin,
                onValueChange = { bottomMargin = it },
                onValueChangeFinished = {
                    onMarginChanged(marginConfig.copy(bottomMm = bottomMargin))
                },
                valueRange = 2f..40f
            )
        }

        // Side margins
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sides", style = MaterialTheme.typography.bodyMedium)
                Text("${sideMargin.toInt()} mm", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = sideMargin,
                onValueChange = { sideMargin = it },
                onValueChangeFinished = {
                    onMarginChanged(
                        marginConfig.copy(
                            leftMm = sideMargin,
                            rightMm = sideMargin
                        )
                    )
                },
                valueRange = 2f..20f
            )
        }

        // Preset buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MarginPresetButton(
                text = "Minimal",
                onClick = { onMarginChanged(MarginConfig.MINIMAL) },
                modifier = Modifier.weight(1f)
            )
            MarginPresetButton(
                text = "Instant Camera",
                onClick = { onMarginChanged(MarginConfig.INSTANT_CAMERA) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MarginPresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Incomplete page mode selector
 */
@Composable
fun IncompletePageModeSelector(
    selectedMode: IncompletePageMode,
    onModeSelected: (IncompletePageMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        Text(
            text = "Last page with fewer photos",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        IncompletePageMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedMode == mode,
                        onClick = { onModeSelected(mode) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = null
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = mode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
