package id.usecase.noted.presentation.components.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

/**
 * A circular icon button designed for camera control interfaces.
 * Provides visual feedback through color animations when selected or pressed.
 *
 * @param icon The icon to display inside the button
 * @param contentDescription Content description for accessibility
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to be applied to the button
 * @param isSelected Whether the button is in a selected state (affects colors)
 * @param containerColor The background color when not selected
 * @param contentColor The icon color when not selected
 * @param selectedContainerColor The background color when selected
 * @param selectedContentColor The icon color when selected
 */
@Composable
fun CameraIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    contentColor: Color = Color.White,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
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

    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = animatedContainerColor,
            contentColor = animatedContentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animatedContentColor
        )
    }
}

@Preview
@Composable
private fun CameraIconButtonPreview() {
    MaterialTheme {
        CameraIconButton(
            icon = Icons.Default.CameraAlt,
            contentDescription = "Camera",
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun CameraIconButtonSelectedPreview() {
    MaterialTheme {
        CameraIconButton(
            icon = Icons.Default.CameraAlt,
            contentDescription = "Camera",
            onClick = {},
            isSelected = true
        )
    }
}
