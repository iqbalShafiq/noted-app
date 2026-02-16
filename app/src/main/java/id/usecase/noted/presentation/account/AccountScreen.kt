package id.usecase.noted.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.usecase.noted.ui.theme.NotedTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountScreenRoot(
    viewModel: AccountViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AccountEffect.NavigateBack -> onNavigateBack()
                AccountEffect.NavigateToLogin -> onNavigateToLogin()
                is AccountEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                    onShowMessage(effect.message)
                }
            }
        }
    }

    AccountScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onNavigateToEditProfile = onNavigateToEditProfile,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    state: AccountState,
    onIntent: (AccountIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToEditProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Akun") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(AccountIntent.NavigateBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.errorMessage != null -> {
                    ErrorContent(
                        errorMessage = state.errorMessage,
                        onRetry = { onIntent(AccountIntent.RefreshProfile) },
                    )
                }

                state.isLoggedIn -> {
                    LoggedInContent(
                        state = state,
                        onIntent = onIntent,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onShowLogoutDialog = { showLogoutDialog = true },
                    )
                }

                else -> {
                    NotLoggedInContent(onIntent = onIntent)
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onIntent(AccountIntent.LogoutClicked)
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Logout") },
        text = { Text("Apakah Anda yakin ingin keluar dari akun?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = errorMessage,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Button(
                    onClick = onRetry,
                ) {
                    Text("Coba Lagi")
                }
            }
        }
    }
}

@Composable
private fun LoggedInContent(
    state: AccountState,
    onIntent: (AccountIntent) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onShowLogoutDialog: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Content yang bisa di-scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 100.dp), // Space untuk bottom bar
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeaderSection(
                displayName = state.displayName,
                username = state.username,
                bio = state.bio,
                email = state.email,
                profilePictureUrl = state.profilePictureUrl,
            )

            AccountInfoSection(
                userId = state.userId,
                createdAtEpochMillis = state.createdAtEpochMillis,
                lastLoginAtEpochMillis = state.lastLoginAtEpochMillis,
                updatedAtEpochMillis = state.updatedAtEpochMillis,
            )

            StatisticsSection(
                totalNotes = state.totalNotes,
                notesShared = state.notesShared,
                notesReceived = state.notesReceived,
                lastSyncAtEpochMillis = state.lastSyncAtEpochMillis,
            )
        }

        // Bottom Bar dengan tombol
        AccountBottomBar(
            onRefresh = { onIntent(AccountIntent.RefreshProfile) },
            onEdit = onNavigateToEditProfile,
            onLogout = onShowLogoutDialog,
            isLoading = state.isLoading,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AccountBottomBar(
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onLogout: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Tombol Refresh (Kiri)
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Profil",
                )
            }

            // Tombol Edit (Tengah)
            OutlinedButton(
                onClick = onEdit,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit")
            }

            // FAB Logout (Kanan)
            FloatingActionButton(
                onClick = onLogout,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
            ) {
                Text(
                    text = "×",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    displayName: String?,
    username: String?,
    bio: String?,
    email: String?,
    profilePictureUrl: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Profile Picture
            if (!profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profilePictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto Profil",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Foto Profil Default",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Display Name
            Text(
                text = displayName ?: username ?: "Pengguna",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            // Username
            if (!username.isNullOrBlank()) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Bio
            if (!bio.isNullOrBlank()) {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Email
            if (!email.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountInfoSection(
    userId: String?,
    createdAtEpochMillis: Long?,
    lastLoginAtEpochMillis: Long?,
    updatedAtEpochMillis: Long?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Informasi Akun",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            userId?.let {
                InfoItem(label = "User ID", value = it)
            }

            createdAtEpochMillis?.let {
                InfoItem(
                    label = "Bergabung Sejak",
                    value = formatDate(it),
                )
            }

            lastLoginAtEpochMillis?.let {
                InfoItem(
                    label = "Login Terakhir",
                    value = formatDateTime(it),
                )
            }

            updatedAtEpochMillis?.let {
                InfoItem(
                    label = "Profil Diperbarui",
                    value = formatDateTime(it),
                )
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatisticsSection(
    totalNotes: Int,
    notesShared: Int,
    notesReceived: Int,
    lastSyncAtEpochMillis: Long?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Statistik",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Stats tanpa icon - horizontal layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(
                    value = totalNotes.toString(),
                    label = "Catatan",
                )
                StatItem(
                    value = notesShared.toString(),
                    label = "Dibagikan",
                )
                StatItem(
                    value = notesReceived.toString(),
                    label = "Diterima",
                )
            }

            // Last Sync
            lastSyncAtEpochMillis?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Sinkron terakhir: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatTime(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotLoggedInContent(onIntent: (AccountIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Anda Belum Login",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "Masuk ke akun Anda untuk mengakses fitur sinkronisasi cloud dan mengelola data Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                OutlinedButton(
                    onClick = { onIntent(AccountIntent.LoginClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Masuk")
                }
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    return SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        .format(Date(epochMillis))
}

private fun formatDateTime(epochMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("id", "ID"))
        .format(Date(epochMillis))
}

private fun formatTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    return when {
        diff < 60_000 -> "Baru saja"
        diff < 3_600_000 -> "${diff / 60_000} menit yang lalu"
        diff < 86_400_000 -> "${diff / 3_600_000} jam yang lalu"
        else -> SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(epochMillis))
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenLoggedInPreview() {
    NotedTheme {
        AccountScreen(
            state = AccountState(
                isLoading = false,
                isLoggedIn = true,
                username = "johndoe",
                userId = "user-123-456",
                displayName = "John Doe",
                bio = "Android developer enthusiast. Love coding and coffee.",
                profilePictureUrl = null,
                email = "john@example.com",
                createdAtEpochMillis = System.currentTimeMillis() - 86400000 * 30,
                lastLoginAtEpochMillis = System.currentTimeMillis() - 3600000,
                updatedAtEpochMillis = System.currentTimeMillis() - 86400000,
                totalNotes = 42,
                notesShared = 5,
                notesReceived = 3,
                lastSyncAtEpochMillis = System.currentTimeMillis() - 1800000,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenNotLoggedInPreview() {
    NotedTheme {
        AccountScreen(
            state = AccountState(
                isLoading = false,
                isLoggedIn = false,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenLoadingPreview() {
    NotedTheme {
        AccountScreen(
            state = AccountState(
                isLoading = true,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenErrorPreview() {
    NotedTheme {
        AccountScreen(
            state = AccountState(
                isLoading = false,
                isLoggedIn = true,
                errorMessage = "Gagal memuat profil. Periksa koneksi internet Anda.",
                username = "johndoe",
            ),
            onIntent = {},
        )
    }
}

