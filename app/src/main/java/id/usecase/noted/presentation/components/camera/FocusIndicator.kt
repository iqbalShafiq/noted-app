package id.usecase.noted.presentation.components.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Focus indicator that appears when user taps to focus.
 *
 * Displays focus bracket corners that animate in (scale down) and
 * fade out smoothly, mimicking the behavior of iOS camera focus indicators.
 *
 * @param x The x-coordinate of the focus point
 * @param y The y-coordinate of the focus point
 * @param isVisible Whether the focus indicator should be displayed
 * @param modifier Modifier to apply to the indicator
 * @param onAnimationComplete Callback invoked when the focus animation completes
 */
@Composable
fun FocusIndicator(
    x: Float,
    y: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    if (!isVisible) return

    val scaleAnimatable = remember { Animatable(1.5f) }
    val alphaAnimatable = remember { Animatable(1.0f) }

    LaunchedEffect(isVisible, x, y) {
        // Reset to initial values
        scaleAnimatable.snapTo(1.5f)
        alphaAnimatable.snapTo(1.0f)

        // Animate scale from 1.5x to 1.0x
        scaleAnimatable.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 300)
        )

        // Animate alpha from 1.0 to 0.0 with delay
        alphaAnimatable.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(
                durationMillis = 1000,
                delayMillis = 500
            )
        )

        onAnimationComplete()
    }

    val scale = scaleAnimatable.value
    val alpha = alphaAnimatable.value
    val sizeDp = 80.dp
    val cornerLength = 20f
    val strokeWidth = 2f

    Canvas(
        modifier = modifier.size(sizeDp)
    ) {
        val indicatorSize = sizeDp.toPx() * scale
        val halfSize = indicatorSize / 2f

        // Calculate actual position (center of canvas is at 0,0 relative to drawing)
        // We draw relative to center, so we don't need to offset by x,y here
        // The parent should position this component at (x, y)

        val color = Color.Yellow.copy(alpha = alpha)

        // Top-left corner
        drawLine(
            color = color,
            start = Offset(-halfSize, -halfSize + cornerLength),
            end = Offset(-halfSize, -halfSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(-halfSize, -halfSize),
            end = Offset(-halfSize + cornerLength, -halfSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Top-right corner
        drawLine(
            color = color,
            start = Offset(halfSize - cornerLength, -halfSize),
            end = Offset(halfSize, -halfSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(halfSize, -halfSize),
            end = Offset(halfSize, -halfSize + cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-right corner
        drawLine(
            color = color,
            start = Offset(halfSize, halfSize - cornerLength),
            end = Offset(halfSize, halfSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(halfSize, halfSize),
            end = Offset(halfSize - cornerLength, halfSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-left corner
        drawLine(
            color = color,
            start = Offset(-halfSize + cornerLength, halfSize),
            end = Offset(-halfSize, halfSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(-halfSize, halfSize),
            end = Offset(-halfSize, halfSize - cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FocusIndicatorPreview() {
    FocusIndicator(
        x = 0f,
        y = 0f,
        isVisible = true
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FocusIndicatorHiddenPreview() {
    FocusIndicator(
        x = 0f,
        y = 0f,
        isVisible = false
    )
}
