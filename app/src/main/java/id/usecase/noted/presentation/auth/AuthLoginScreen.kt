package id.usecase.noted.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
fun AuthLoginScreenRoot(
    viewModel: AuthViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthEffect.NavigateBack -> onNavigateBack()
                AuthEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
                AuthEffect.NavigateToRegister -> onNavigateToRegister()
                AuthEffect.NavigateToLogin -> Unit
                is AuthEffect.ShowMessage -> onShowMessage(effect.message)
            }
        }
    }

    AuthLoginScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun AuthLoginScreen(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormLayout(modifier = modifier) {
        Spacer(modifier = Modifier.height(32.dp))

        AuthHeader(
            title = "Selamat Datang",
            subtitle = "Masuk ke akun Noted Anda",
            icon = Icons.Default.Lock,
        )

        // Login form
        LoginFormContent(
            state = state,
            onIntent = onIntent,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Register link
        val annotatedString = buildAnnotatedString {
            append("Belum punya akun? ")
            pushStringAnnotation(tag = "register", annotation = "register")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append("Daftar")
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
                    tag = "register",
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let {
                    onIntent(AuthIntent.OpenRegisterClicked)
                }
            },
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LoginFormContent(
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
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            visualTransformation = PasswordVisualTransformation(),
        )

        // Forgot password link
        TextButton(
            onClick = { onIntent(AuthIntent.OpenForgotPasswordClicked) },
            enabled = !state.isSubmitting,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Lupa password?")
        }

        // Submit button
        AuthSubmitButton(
            text = "Masuk",
            onClick = { onIntent(AuthIntent.LoginSubmitClicked) },
            isLoading = state.isSubmitting,
            enabled = !state.isSubmitting,
        )

        // Error message
        state.syncStatus.lastErrorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthLoginScreenPreview() {
    NotedTheme {
        AuthLoginScreen(
            state = AuthState(
                usernameInput = "tester",
                passwordInput = "password123",
            ),
            onIntent = {},
        )
    }
}
