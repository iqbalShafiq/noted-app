package id.usecase.noted.presentation.components.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.usecase.noted.ui.theme.NotedTheme

/**
 * Standard bottom app bar with consistent styling.
 *
 * @param modifier Modifier to be applied
 * @param tonalElevation Elevation of the bottom app bar (default 3.dp)
 * @param actions Content for the bottom app bar actions area
 * @param floatingActionButton Optional FAB to display
 */
@Composable
fun NotedBottomAppBar(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 3.dp,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable (() -> Unit)? = null,
) {
    BottomAppBar(
        modifier = modifier,
        tonalElevation = tonalElevation,
        actions = actions,
        floatingActionButton = floatingActionButton,
    )
}

/**
 * Standard FAB for use with NotedBottomAppBar.
 *
 * @param onClick Callback when FAB is clicked
 * @param modifier Modifier to be applied
 * @param content FAB content (typically Icon)
 */
@Composable
fun NotedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = FloatingActionButtonDefaults.shape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun NotedBottomAppBarPreview() {
    NotedTheme {
        NotedBottomAppBar(
            floatingActionButton = {
                NotedFloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            },
            actions = {}
        )
    }
}
