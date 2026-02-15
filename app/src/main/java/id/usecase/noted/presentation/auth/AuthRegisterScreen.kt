package id.usecase.noted.presentation.auth

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
fun AuthRegisterScreenRoot(
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

    AuthRegisterScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun AuthRegisterScreen(
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
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Buat Akun",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Daftar untuk mulai menggunakan Noted",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Register form
        RegisterFormContent(
            state = state,
            onIntent = onIntent,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login link
        val annotatedString = buildAnnotatedString {
            append("Sudah punya akun? ")
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
private fun RegisterFormContent(
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
            placeholder = { Text("Pilih username") },
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
            placeholder = { Text("Minimal 8 karakter") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                )
            },
            supportingText = {
                Text("Password harus minimal 8 karakter")
            },
            shape = MaterialTheme.shapes.medium,
        )

        OutlinedTextField(
            value = state.confirmPasswordInput,
            onValueChange = { value -> onIntent(AuthIntent.ConfirmPasswordChanged(value)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting,
            label = { Text("Konfirmasi Password") },
            placeholder = { Text("Ulangi password") },
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

        Spacer(modifier = Modifier.height(8.dp))

        // Submit button
        Button(
            onClick = { onIntent(AuthIntent.RegisterSubmitClicked) },
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
                Text("Daftar")
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
private fun AuthRegisterScreenPreview() {
    NotedTheme {
        AuthRegisterScreen(
            state = AuthState(
                usernameInput = "new-user",
                passwordInput = "password123",
                confirmPasswordInput = "password123",
            ),
            onIntent = {},
        )
    }
}
