package id.usecase.noted.presentation.components.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A vertical slider for controlling camera exposure compensation.
 * Displays current exposure value and allows fine-tuning of image brightness.
 *
 * @param exposure Current exposure compensation value (e.g., -5, 0, +3)
 * @param onExposureChange Callback invoked when exposure value changes
 * @param modifier Modifier to be applied to the slider container
 * @param minExposure Minimum exposure compensation allowed
 * @param maxExposure Maximum exposure compensation allowed
 */
@Composable
fun CameraExposureSlider(
    exposure: Int,
    onExposureChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minExposure: Int = -10,
    maxExposure: Int = 10
) {
    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight(0.6f)
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 4.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = exposure.toFloat(),
                    onValueChange = { value ->
                        onExposureChange(value.toInt())
                    },
                    valueRange = minExposure.toFloat()..maxExposure.toFloat(),
                    modifier = Modifier
                        .width(200.dp)
                        .rotate(270f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }

            Text(
                text = formatExposureValue(exposure),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Formats exposure value for display, adding + sign for positive values.
 */
private fun formatExposureValue(exposure: Int): String {
    return if (exposure > 0) "+$exposure" else exposure.toString()
}

@Preview
@Composable
private fun CameraExposureSliderPreview() {
    MaterialTheme {
        CameraExposureSlider(
            exposure = 0,
            onExposureChange = {}
        )
    }
}

@Preview
@Composable
private fun CameraExposureSliderPositivePreview() {
    MaterialTheme {
        CameraExposureSlider(
            exposure = 5,
            onExposureChange = {}
        )
    }
}

@Preview
@Composable
private fun CameraExposureSliderNegativePreview() {
    MaterialTheme {
        CameraExposureSlider(
            exposure = -3,
            onExposureChange = {}
        )
    }
}
