package id.usecase.noted.presentation.components.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A horizontal row layout for arranging camera control buttons.
 * Provides consistent spacing and alignment for camera interface controls.
 *
 * @param modifier Modifier to be applied to the row
 * @param horizontalArrangement The horizontal arrangement of the content
 * @param content The composable content to display inside the row
 */
@Composable
fun CameraControlRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Preview
@Composable
private fun CameraControlRowPreview() {
    MaterialTheme {
        CameraControlRow {
            CameraIconButton(
                icon = Icons.Default.FlashOn,
                contentDescription = "Flash",
                onClick = {}
            )
            CameraIconButton(
                icon = Icons.Default.CameraAlt,
                contentDescription = "Capture",
                onClick = {}
            )
            CameraIconButton(
                icon = Icons.Default.FlipCameraAndroid,
                contentDescription = "Switch Camera",
                onClick = {}
            )
        }
    }
}
