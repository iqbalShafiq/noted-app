package id.usecase.noted.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import id.usecase.noted.presentation.auth.AuthForgotPasswordScreenRoot
import id.usecase.noted.presentation.auth.AuthLoginScreenRoot
import id.usecase.noted.presentation.auth.AuthRegisterScreenRoot
import id.usecase.noted.presentation.auth.AuthViewModel
import id.usecase.noted.presentation.note.editor.NoteEditorIntent
import id.usecase.noted.presentation.note.editor.NoteEditorScreenRoot
import id.usecase.noted.presentation.note.editor.NoteEditorViewModel
import id.usecase.noted.presentation.note.editor.camera.NoteCameraScreenRoot
import id.usecase.noted.presentation.note.editor.location.NoteLocationPickerScreen
import id.usecase.noted.presentation.note.list.NoteListScreenRoot
import id.usecase.noted.presentation.note.list.NoteListViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun NoteNavigation(
    modifier: Modifier = Modifier,
) {
    val listViewModel: NoteListViewModel = koinViewModel()
    val editorViewModel: NoteEditorViewModel = koinViewModel()
    val authViewModel: AuthViewModel = koinViewModel()
    val backStack = rememberNavBackStack(NoteListNavKey)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            entryProvider = entryProvider {
                entry<NoteListNavKey> {
                    NoteListScreenRoot(
                        viewModel = listViewModel,
                        onShowMessage = ::showMessage,
                        onNavigateToAuth = {
                            if (backStack.lastOrNull() != AuthLoginNavKey) {
                                backStack.add(AuthLoginNavKey)
                            }
                        },
                        onNavigateToEditor = { noteId ->
                            editorViewModel.onIntent(NoteEditorIntent.EditorOpened(noteId))
                            moveToRootDestination(
                                backStack = backStack,
                                destination = NoteEditorNavKey(noteId = noteId),
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                entry<AuthLoginNavKey> {
                    AuthLoginScreenRoot(
                        viewModel = authViewModel,
                        onShowMessage = ::showMessage,
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                            if (backStack.isEmpty()) {
                                backStack.add(NoteListNavKey)
                            }
                        },
                        onNavigateToRegister = {
                            if (backStack.lastOrNull() != AuthRegisterNavKey) {
                                backStack.add(AuthRegisterNavKey)
                            }
                        },
                        onNavigateToForgotPassword = {
                            if (backStack.lastOrNull() != AuthForgotPasswordNavKey) {
                                backStack.add(AuthForgotPasswordNavKey)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                entry<AuthRegisterNavKey> {
                    AuthRegisterScreenRoot(
                        viewModel = authViewModel,
                        onShowMessage = ::showMessage,
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                            if (backStack.isEmpty()) {
                                backStack.add(NoteListNavKey)
                            }
                        },
                        onNavigateToLogin = {
                            moveToAuthLoginDestination(backStack)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                entry<AuthForgotPasswordNavKey> {
                    AuthForgotPasswordScreenRoot(
                        viewModel = authViewModel,
                        onShowMessage = ::showMessage,
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                            if (backStack.isEmpty()) {
                                backStack.add(NoteListNavKey)
                            }
                        },
                        onNavigateToLogin = {
                            moveToAuthLoginDestination(backStack)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                entry<NoteEditorNavKey> {
                    NoteEditorScreenRoot(
                        viewModel = editorViewModel,
                        onShowMessage = ::showMessage,
                        onNavigateToList = {
                            moveToRootDestination(
                                backStack = backStack,
                                destination = NoteListNavKey,
                            )
                        },
                        onNavigateToCamera = {
                            if (backStack.lastOrNull() != NoteCameraNavKey) {
                                backStack.add(NoteCameraNavKey)
                            }
                        },
                        onNavigateToLocationPicker = { initialLocation ->
                            val destination = NoteLocationPickerNavKey(
                                latitude = initialLocation?.latitude,
                                longitude = initialLocation?.longitude,
                                label = initialLocation?.label,
                            )
                            if (backStack.lastOrNull() != destination) {
                                backStack.add(destination)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                entry<NoteCameraNavKey> {
                    NoteCameraScreenRoot(
                        onPhotoCaptured = { imageUri ->
                            editorViewModel.onIntent(NoteEditorIntent.PhotoPicked(imageUri))
                            backStack.removeLastOrNull()
                        },
                        onShowMessage = ::showMessage,
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                entry<NoteLocationPickerNavKey> { key ->
                    NoteLocationPickerScreen(
                        initialLatitude = key.latitude,
                        initialLongitude = key.longitude,
                        initialLabel = key.label,
                        onTagLocation = { latitude, longitude, label ->
                            editorViewModel.onIntent(
                                NoteEditorIntent.LocationTagged(
                                    latitude = latitude,
                                    longitude = longitude,
                                    label = label,
                                ),
                            )
                            backStack.removeLastOrNull()
                        },
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
    }
}

private fun moveToRootDestination(
    backStack: NavBackStack<NavKey>,
    destination: NavKey,
) {
    if (backStack.size == 1 && backStack.lastOrNull() == destination) {
        return
    }

    backStack.clear()
    backStack.add(destination)
}

private fun moveToAuthLoginDestination(backStack: NavBackStack<NavKey>) {
    if (backStack.lastOrNull() == AuthLoginNavKey) {
        return
    }

    if (backStack.lastOrNull() == AuthRegisterNavKey || backStack.lastOrNull() == AuthForgotPasswordNavKey) {
        backStack.removeLastOrNull()
    }

    if (backStack.lastOrNull() != AuthLoginNavKey) {
        backStack.add(AuthLoginNavKey)
    }
}
