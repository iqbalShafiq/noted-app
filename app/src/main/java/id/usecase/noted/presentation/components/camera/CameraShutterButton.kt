package id.usecase.noted.presentation.components.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * An animated shutter button for camera capture with micro-interactions.
 * Provides visual feedback through scale animations and color transitions.
 *
 * @param onClick Callback invoked when the button is clicked (tap)
 * @param onLongPress Callback invoked when the button is long-pressed (burst mode)
 * @param modifier Modifier to be applied to the button
 * @param isCapturing Whether the camera is currently capturing an image
 * @param isBurstMode Whether burst mode is active (affects outer ring color)
 * @param size Diameter of the shutter button in dp
 */
@Composable
fun CameraShutterButton(
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isCapturing: Boolean = false,
    isBurstMode: Boolean = false,
    size: Int = 80
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnimatable = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        scaleAnimatable.animateTo(
            targetValue = if (isPressed) 0.9f else 1f,
            animationSpec = tween(150)
        )
    }

    val outerRingColor by animateColorAsState(
        targetValue = if (isBurstMode) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.White
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "outer_ring_color"
    )

    val innerCircleColor by animateColorAsState(
        targetValue = if (isCapturing) {
            Color.Red
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "inner_circle_color"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .scale(scaleAnimatable.value)
            .border(
                width = 4.dp,
                color = outerRingColor,
                shape = CircleShape
            )
            .background(
                color = Color.Transparent,
                shape = CircleShape
            )
            .pointerInput(onClick, onLongPress) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress?.invoke() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((size - 16).dp)
                .background(
                    color = innerCircleColor,
                    shape = CircleShape
                )
        )
    }
}

@Preview
@Composable
private fun CameraShutterButtonPreview() {
    MaterialTheme {
        CameraShutterButton(
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun CameraShutterButtonCapturingPreview() {
    MaterialTheme {
        CameraShutterButton(
            onClick = {},
            isCapturing = true
        )
    }
}

@Preview
@Composable
private fun CameraShutterButtonBurstModePreview() {
    MaterialTheme {
        CameraShutterButton(
            onClick = {},
            isBurstMode = true
        )
    }
}

@Preview
@Composable
private fun CameraShutterButtonBurstAndCapturingPreview() {
    MaterialTheme {
        CameraShutterButton(
            onClick = {},
            isBurstMode = true,
            isCapturing = true
        )
    }
}
