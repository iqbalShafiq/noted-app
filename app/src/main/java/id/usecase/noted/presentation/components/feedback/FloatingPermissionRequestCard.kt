package id.usecase.noted.presentation.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.usecase.noted.ui.theme.NotedTheme

/**
 * Floating card for requesting runtime permission from users.
 */
@Composable
fun FloatingPermissionRequestCard(
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FloatingPermissionRequestCardPreview() {
    NotedTheme {
        FloatingPermissionRequestCard(
            title = "Akses Lokasi",
            message = "Izin lokasi dibutuhkan untuk menentukan posisi saat ini di peta.",
            actionLabel = "Izinkan Lokasi",
            onActionClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}
