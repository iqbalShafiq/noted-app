package id.usecase.noted.presentation.components.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A horizontal slider for controlling camera zoom level.
 * Displays current zoom ratio and provides smooth zoom adjustment.
 *
 * @param zoomRatio Current zoom ratio value (e.g., 1.0f, 2.5f)
 * @param onZoomChange Callback invoked when zoom value changes
 * @param modifier Modifier to be applied to the slider container
 * @param minZoom Minimum zoom ratio allowed
 * @param maxZoom Maximum zoom ratio allowed
 */
@Composable
fun CameraZoomSlider(
    zoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minZoom: Float = 1.0f,
    maxZoom: Float = 5.0f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = zoomRatio,
                onValueChange = onZoomChange,
                valueRange = minZoom..maxZoom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Text(
                text = "${zoomRatio.toFixed(1)}x",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/**
 * Formats a float to one decimal place.
 */
private fun Float.toFixed(decimals: Int): String {
    return String.format("%.${decimals}f", this)
}

@Preview
@Composable
private fun CameraZoomSliderPreview() {
    MaterialTheme {
        CameraZoomSlider(
            zoomRatio = 2.5f,
            onZoomChange = {}
        )
    }
}

@Preview
@Composable
private fun CameraZoomSliderMinZoomPreview() {
    MaterialTheme {
        CameraZoomSlider(
            zoomRatio = 1.0f,
            onZoomChange = {}
        )
    }
}

@Preview
@Composable
private fun CameraZoomSliderMaxZoomPreview() {
    MaterialTheme {
        CameraZoomSlider(
            zoomRatio = 5.0f,
            onZoomChange = {}
        )
    }
}
