package id.usecase.noted.presentation.components.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun CameraIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    containerColor: Color = Color.Black.copy(alpha = 0.5f),
    contentColor: Color = Color.White,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "scale_animation"
    )

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isSelected) selectedContainerColor else containerColor,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "container_color_animation"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) selectedContentColor else contentColor,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "content_color_animation"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(animatedScale)
            .background(
                color = animatedContainerColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animatedContentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}