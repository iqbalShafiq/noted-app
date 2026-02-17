package id.usecase.noted.presentation.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.usecase.noted.ui.theme.NotedTheme

/**
 * Centered loading indicator for screens and sections.
 *
 * @param modifier Modifier to be applied to the container
 * @param size Size of the progress indicator (default 48dp)
 * @param strokeWidth Width of the progress indicator stroke (default 4dp)
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = strokeWidth,
        )
    }
}

/**
 * Loading state with custom content slot.
 *
 * @param modifier Modifier to be applied to the container
 * @param content Custom content to display in the loading state
 */
@Composable
fun LoadingStateBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    NotedTheme {
        LoadingState()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStateBoxPreview() {
    NotedTheme {
        LoadingStateBox {
            CircularProgressIndicator()
        }
    }
}
