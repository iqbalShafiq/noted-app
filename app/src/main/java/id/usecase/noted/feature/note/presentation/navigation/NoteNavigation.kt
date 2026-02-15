package id.usecase.noted.feature.note.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewmodel.compose.viewModel
import id.usecase.noted.feature.note.data.NoteRepositoryProvider
import id.usecase.noted.feature.note.presentation.editor.NoteEditorIntent
import id.usecase.noted.feature.note.presentation.editor.NoteEditorScreenRoot
import id.usecase.noted.feature.note.presentation.editor.NoteEditorViewModel
import id.usecase.noted.feature.note.presentation.editor.camera.NoteCameraScreenRoot
import id.usecase.noted.feature.note.presentation.editor.location.NoteLocationPickerScreen
import id.usecase.noted.feature.note.presentation.list.NoteListScreenRoot
import id.usecase.noted.feature.note.presentation.list.NoteListViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch

@Composable
fun NoteNavigation(
    modifier: Modifier = Modifier,
) {
    val applicationContext = LocalContext.current.applicationContext
    val noteRepository = remember(applicationContext) {
        NoteRepositoryProvider.provide(applicationContext)
    }
    val listViewModel: NoteListViewModel = viewModel(
        factory = NoteListViewModel.factory(
            noteRepository = noteRepository,
            noteSyncCoordinator = noteRepository,
        ),
    )
    val editorViewModel: NoteEditorViewModel = viewModel(
        factory = NoteEditorViewModel.factory(noteRepository),
    )
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
