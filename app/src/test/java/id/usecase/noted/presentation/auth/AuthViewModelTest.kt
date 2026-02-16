package id.usecase.noted.presentation.auth

import id.usecase.noted.data.sync.NoteSyncCoordinator
import id.usecase.noted.data.sync.NoteSyncStatus
import id.usecase.noted.data.sync.UserSession
import id.usecase.noted.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loginSubmitCallsLoginOnCoordinator() = runTest {
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = AuthViewModel(noteSyncCoordinator = syncCoordinator)

        viewModel.onIntent(AuthIntent.UsernameChanged("qa-user"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.LoginSubmitClicked)
        advanceUntilIdle()

        assertEquals("qa-user", syncCoordinator.lastLoginUsername)
        assertEquals("password123", syncCoordinator.lastLoginPassword)
    }

    @Test
    fun registerSubmitCallsRegisterOnCoordinator() = runTest {
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = AuthViewModel(noteSyncCoordinator = syncCoordinator)

        viewModel.onIntent(AuthIntent.UsernameChanged("new-user"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.RegisterSubmitClicked)
        advanceUntilIdle()

        assertEquals("new-user", syncCoordinator.lastRegisterUsername)
        assertEquals("password123", syncCoordinator.lastRegisterPassword)
    }

    @Test
    fun forgotPasswordSubmitCallsForgotPasswordAndNavigatesToLogin() = runTest {
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = AuthViewModel(noteSyncCoordinator = syncCoordinator)
        val effects = mutableListOf<AuthEffect>()
        val effectJob = launch {
            viewModel.effect.take(2).toList(effects)
        }

        viewModel.onIntent(AuthIntent.UsernameChanged("lost-user"))
        viewModel.onIntent(AuthIntent.PasswordChanged("newpassword123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("newpassword123"))
        viewModel.onIntent(AuthIntent.ForgotPasswordSubmitClicked)
        advanceUntilIdle()

        assertEquals("lost-user", syncCoordinator.lastForgotUsername)
        assertEquals("newpassword123", syncCoordinator.lastForgotPassword)
        assertEquals(AuthEffect.NavigateToLogin, effects.last())

        effectJob.cancel()
    }

    @Test
    fun registerSubmitWithDifferentConfirmationEmitsValidationError() = runTest {
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = AuthViewModel(noteSyncCoordinator = syncCoordinator)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(AuthIntent.UsernameChanged("qa-user"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("different123"))
        viewModel.onIntent(AuthIntent.RegisterSubmitClicked)
        advanceUntilIdle()

        assertEquals(
            AuthEffect.ShowMessage("Konfirmasi password tidak sama"),
            firstEffect.await(),
        )
    }

    @Test
    fun openRegisterClickedEmitsNavigateToRegister() = runTest {
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = AuthViewModel(noteSyncCoordinator = syncCoordinator)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(AuthIntent.OpenRegisterClicked)
        advanceUntilIdle()

        assertEquals(AuthEffect.NavigateToRegister, firstEffect.await())
    }

    @Test
    fun openForgotPasswordClickedEmitsNavigateToForgotPassword() = runTest {
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = AuthViewModel(noteSyncCoordinator = syncCoordinator)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(AuthIntent.OpenForgotPasswordClicked)
        advanceUntilIdle()

        assertEquals(AuthEffect.NavigateToForgotPassword, firstEffect.await())
    }

    @Test
    fun logoutClickedCallsSignOut() = runTest {
        val syncCoordinator = FakeSyncCoordinator().apply {
            updateSyncStatus(
                NoteSyncStatus(
                    userId = "user-1",
                    username = "qa-user",
                ),
            )
        }
        val viewModel = AuthViewModel(noteSyncCoordinator = syncCoordinator)

        viewModel.onIntent(AuthIntent.LogoutClicked)
        advanceUntilIdle()

        assertEquals(1, syncCoordinator.signOutCallCount)
        assertTrue(viewModel.state.value.passwordInput.isEmpty())
    }
}

private class FakeSyncCoordinator : NoteSyncCoordinator {
    override val session: StateFlow<UserSession> = MutableStateFlow(UserSession(userId = null, deviceId = "device"))

    private val mutableSyncStatus = MutableStateFlow(NoteSyncStatus())
    override val syncStatus: StateFlow<NoteSyncStatus> = mutableSyncStatus

    var lastRegisterUsername: String? = null
    var lastRegisterPassword: String? = null
    var lastLoginUsername: String? = null
    var lastLoginPassword: String? = null
    var lastForgotUsername: String? = null
    var lastForgotPassword: String? = null
    var signOutCallCount: Int = 0

    override suspend fun register(username: String, password: String) {
        lastRegisterUsername = username
        lastRegisterPassword = password
    }

    override suspend fun login(username: String, password: String) {
        lastLoginUsername = username
        lastLoginPassword = password
    }

    override suspend fun forgotPassword(username: String, newPassword: String) {
        lastForgotUsername = username
        lastForgotPassword = newPassword
    }

    override suspend fun signOut() {
        signOutCallCount += 1
    }

    override suspend fun syncNow() = Unit

    override suspend fun uploadPendingNow() = Unit

    override suspend fun importNow() = Unit

    fun updateSyncStatus(status: NoteSyncStatus) {
        mutableSyncStatus.value = status
    }
}
