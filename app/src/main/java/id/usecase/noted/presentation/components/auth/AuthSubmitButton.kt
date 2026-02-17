package id.usecase.noted.presentation.components.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.usecase.noted.ui.theme.NotedTheme

/**
 * Standard submit button for authentication forms.
 *
 * @param text Button text
 * @param onClick Callback when clicked
 * @param isLoading Whether loading state (shows progress indicator)
 * @param enabled Whether button is enabled
 * @param modifier Modifier to be applied
 */
@Composable
fun AuthSubmitButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.height(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthSubmitButtonPreview() {
    NotedTheme {
        AuthSubmitButton(
            text = "Masuk",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthSubmitButtonLoadingPreview() {
    NotedTheme {
        AuthSubmitButton(
            text = "Masuk",
            onClick = {},
            isLoading = true
        )
    }
}
