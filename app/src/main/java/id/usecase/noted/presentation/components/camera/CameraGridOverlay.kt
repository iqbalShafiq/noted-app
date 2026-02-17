package id.usecase.noted.presentation.components.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview

/**
 * Overlay that draws a rule-of-thirds grid for camera composition.
 *
 * The grid consists of 2 vertical lines (at 1/3 and 2/3 width) and
 * 2 horizontal lines (at 1/3 and 2/3 height), commonly used in photography
 * for balanced composition.
 *
 * @param isVisible Whether the grid overlay should be displayed
 * @param modifier Modifier to apply to the overlay
 * @param color The color of the grid lines (defaults to semi-transparent white)
 * @param strokeWidth The width of the grid lines in pixels
 */
@Composable
fun CameraGridOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.5f),
    strokeWidth: Float = 1f
) {
    if (!isVisible) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val oneThirdX = width / 3f
        val twoThirdsX = width * 2f / 3f
        val oneThirdY = height / 3f
        val twoThirdsY = height * 2f / 3f

        // Vertical lines
        drawLine(
            color = color,
            start = Offset(oneThirdX, 0f),
            end = Offset(oneThirdX, height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(twoThirdsX, 0f),
            end = Offset(twoThirdsX, height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Horizontal lines
        drawLine(
            color = color,
            start = Offset(0f, oneThirdY),
            end = Offset(width, oneThirdY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(0f, twoThirdsY),
            end = Offset(width, twoThirdsY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CameraGridOverlayVisiblePreview() {
    CameraGridOverlay(
        isVisible = true
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CameraGridOverlayHiddenPreview() {
    CameraGridOverlay(
        isVisible = false
    )
}
