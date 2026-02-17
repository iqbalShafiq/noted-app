package id.usecase.noted.presentation.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import id.usecase.noted.ui.theme.NotedTheme

/**
 * Standard top app bar with back navigation.
 *
 * @param title Title text to display
 * @param onNavigateBack Callback when back button is clicked
 * @param modifier Modifier to be applied
 * @param actions Optional trailing actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotedTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                )
            }
        },
        actions = { actions?.invoke() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * Top app bar without back navigation.
 *
 * @param title Title text to display
 * @param modifier Modifier to be applied
 * @param actions Optional trailing actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotedTopAppBarStatic(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        actions = { actions?.invoke() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NotedTopAppBarPreview() {
    NotedTheme {
        NotedTopAppBar(
            title = "Detail Catatan",
            onNavigateBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NotedTopAppBarStaticPreview() {
    NotedTheme {
        NotedTopAppBarStatic(
            title = "Beranda"
        )
    }
}