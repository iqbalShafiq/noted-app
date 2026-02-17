package id.usecase.noted.presentation.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.usecase.noted.presentation.components.auth.AuthFormContainer
import id.usecase.noted.presentation.components.auth.AuthFormLayout
import id.usecase.noted.presentation.components.auth.AuthHeader
import id.usecase.noted.presentation.components.auth.AuthSubmitButton
import id.usecase.noted.presentation.components.auth.AuthTextField
import id.usecase.noted.ui.theme.NotedTheme

@Composable
fun AuthForgotPasswordScreenRoot(
    viewModel: AuthViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthEffect.NavigateBack -> onNavigateBack()
                AuthEffect.NavigateToLogin -> onNavigateToLogin()
                AuthEffect.NavigateToForgotPassword -> Unit
                AuthEffect.NavigateToRegister -> Unit
                is AuthEffect.ShowMessage -> onShowMessage(effect.message)
            }
        }
    }

    AuthForgotPasswordScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun AuthForgotPasswordScreen(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormLayout(modifier = modifier) {
        Spacer(modifier = Modifier.height(32.dp))

        AuthHeader(
            title = "Reset Password",
            subtitle = "Masukkan username dan password baru Anda",
            icon = Icons.Default.Refresh,
        )

        // Form content
        ForgotPasswordFormContent(
            state = state,
            onIntent = onIntent,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Back to login link
        val annotatedString = buildAnnotatedString {
            append("Ingat password? ")
            pushStringAnnotation(tag = "login", annotation = "login")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append("Masuk")
            }
            pop()
        }

        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    tag = "login",
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let {
                    onIntent(AuthIntent.OpenLoginClicked)
                }
            },
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ForgotPasswordFormContent(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    AuthFormContainer {
        AuthTextField(
            value = state.usernameInput,
            onValueChange = { value -> onIntent(AuthIntent.UsernameChanged(value)) },
            label = "Username",
            leadingIcon = Icons.Default.Person,
        )

        AuthTextField(
            value = state.passwordInput,
            onValueChange = { value -> onIntent(AuthIntent.PasswordChanged(value)) },
            label = "Password Baru",
            leadingIcon = Icons.Default.Lock,
            visualTransformation = PasswordVisualTransformation(),
        )

        AuthTextField(
            value = state.confirmPasswordInput,
            onValueChange = { value -> onIntent(AuthIntent.ConfirmPasswordChanged(value)) },
            label = "Konfirmasi Password Baru",
            leadingIcon = Icons.Default.Lock,
            visualTransformation = PasswordVisualTransformation(),
        )

        // Submit button
        AuthSubmitButton(
            text = "Reset Password",
            onClick = { onIntent(AuthIntent.ForgotPasswordSubmitClicked) },
            isLoading = state.isSubmitting,
            enabled = !state.isSubmitting,
        )

        // Error message
        state.syncStatus.lastErrorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthForgotPasswordScreenPreview() {
    NotedTheme {
        AuthForgotPasswordScreen(
            state = AuthState(
                usernameInput = "tester",
                passwordInput = "newpassword123",
                confirmPasswordInput = "newpassword123",
            ),
            onIntent = {},
        )
    }
}
