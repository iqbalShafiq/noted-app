package id.usecase.noted.presentation.components.camera

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Level indicator that shows camera orientation with smooth animations.
 *
 * Displays a center dot and a roll indicator line that animates smoothly
 * to indicate the camera's roll angle. Color changes to green when level
 * (within threshold) and yellow otherwise.
 *
 * @param isVisible Whether the level indicator should be displayed
 * @param modifier Modifier to apply to the indicator
 * @param pitch The pitch angle in degrees (not currently displayed but available for future use)
 * @param roll The roll angle in degrees (-180 to 180)
 * @param thresholdDegrees The threshold in degrees within which the camera is considered level
 */
@Composable
fun CameraLevelIndicator(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    pitch: Float = 0f,
    roll: Float = 0f,
    thresholdDegrees: Float = 2f
) {
    if (!isVisible) return

    val animatedRoll by animateFloatAsState(
        targetValue = roll,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "roll"
    )

    val isLevel = abs(animatedRoll) <= thresholdDegrees
    val indicatorColor = if (isLevel) Color.Green else Color.Yellow

    Canvas(modifier = modifier.size(100.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.width.coerceAtMost(size.height) / 4f

        // Draw center dot
        drawCircle(
            color = indicatorColor,
            radius = 4f,
            center = Offset(centerX, centerY)
        )

        // Calculate line endpoints based on roll
        val angleRad = Math.toRadians(animatedRoll.toDouble())
        val cos = kotlin.math.cos(angleRad).toFloat()
        val sin = kotlin.math.sin(angleRad).toFloat()

        val startX = centerX - radius * cos
        val startY = centerY - radius * sin
        val endX = centerX + radius * cos
        val endY = centerY + radius * sin

        // Draw roll indicator line
        drawLine(
            color = indicatorColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CameraLevelIndicatorLevelPreview() {
    CameraLevelIndicator(
        isVisible = true,
        roll = 0f
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CameraLevelIndicatorTiltedPreview() {
    CameraLevelIndicator(
        isVisible = true,
        roll = 15f
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CameraLevelIndicatorHiddenPreview() {
    CameraLevelIndicator(
        isVisible = false
    )
}
