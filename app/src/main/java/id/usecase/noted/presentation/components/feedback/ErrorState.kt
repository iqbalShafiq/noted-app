package id.usecase.noted.presentation.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.usecase.noted.ui.theme.NotedTheme

/**
 * Error state display with message and retry action.
 *
 * @param message Error message to display
 * @param onRetry Callback when retry button is clicked
 * @param modifier Modifier to be applied
 * @param retryButtonText Text for retry button (default "Coba Lagi")
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryButtonText: String = "Coba Lagi",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(onClick = onRetry) {
            Text(retryButtonText)
        }
    }
}

/**
 * Error state without retry button for non-recoverable errors.
 *
 * @param message Error message to display
 * @param modifier Modifier to be applied
 */
@Composable
fun ErrorStateStatic(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    NotedTheme {
        ErrorState(
            message = "Terjadi kesalahan saat memuat data",
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateStaticPreview() {
    NotedTheme {
        ErrorStateStatic(
            message = "Data tidak dapat dimuat"
        )
    }
}
