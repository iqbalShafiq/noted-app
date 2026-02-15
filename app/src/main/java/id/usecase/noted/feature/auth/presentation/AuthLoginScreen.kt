package id.usecase.noted.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // App Logo/Icon area
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Selamat Datang",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Masuk ke akun Noted Anda",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Login form
        LoginFormContent(
            state = state,
            onIntent = onIntent,
        )

        Spacer(modifier = Modifier.height(24.dp))

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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.usernameInput,
            onValueChange = { value -> onIntent(AuthIntent.UsernameChanged(value)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting,
            label = { Text("Username") },
            placeholder = { Text("Masukkan username") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                )
            },
            shape = MaterialTheme.shapes.medium,
        )

        OutlinedTextField(
            value = state.passwordInput,
            onValueChange = { value -> onIntent(AuthIntent.PasswordChanged(value)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting,
            label = { Text("Password") },
            placeholder = { Text("Masukkan password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                )
            },
            shape = MaterialTheme.shapes.medium,
        )

        // Forgot password link
        TextButton(
            onClick = { onIntent(AuthIntent.OpenForgotPasswordClicked) },
            enabled = !state.isSubmitting,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Lupa password?")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Submit button
        Button(
            onClick = { onIntent(AuthIntent.LoginSubmitClicked) },
            enabled = !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Masuk")
            }
        }

        // Error message
        state.syncStatus.lastErrorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
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
