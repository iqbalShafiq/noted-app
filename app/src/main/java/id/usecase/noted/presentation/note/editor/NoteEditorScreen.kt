package id.usecase.noted.presentation.note.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.usecase.noted.presentation.note.editor.preview.NoteEditorPreviewData
import id.usecase.noted.ui.theme.NotedTheme
import java.util.Locale

@Composable
fun NoteEditorScreenRoot(
    viewModel: NoteEditorViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToLocationPicker: (NoteEditorLocation?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris ->
        uris.forEach { uri ->
            viewModel.onIntent(NoteEditorIntent.PhotoPicked(uri.toString()))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NoteEditorEffect.NavigateToCamera -> onNavigateToCamera()
                is NoteEditorEffect.NavigateToLocationPicker -> {
                    onNavigateToLocationPicker(effect.initialLocation)
                }
                NoteEditorEffect.LaunchPhotoPicker -> {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }

                is NoteEditorEffect.ShowMessage -> onShowMessage(effect.message)
                NoteEditorEffect.NavigateToList -> onNavigateToList()
            }
        }
    }

    NoteEditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateToList,
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NoteEditorScreen(
    state: NoteEditorState,
    onIntent: (NoteEditorIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (state.editingNoteId == null) "Note Editor" else "Edit Note"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { onIntent(NoteEditorIntent.InsertPhotoFromCameraClicked) }) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Ambil foto dari kamera",
                        )
                    }
                    IconButton(onClick = { onIntent(NoteEditorIntent.InsertPhotoFromGalleryClicked) }) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Pilih foto dari galeri",
                        )
                    }
                    IconButton(onClick = { onIntent(NoteEditorIntent.TagLocationClicked) }) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = "Tag lokasi",
                        )
                    }
                    if (state.editingNoteId != null) {
                        IconButton(onClick = { onIntent(NoteEditorIntent.DeleteClicked) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Hapus note",
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = BottomAppBarDefaults.ContainerElevation,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (!state.isSaving && !state.isLoadingNote) {
                                onIntent(NoteEditorIntent.SaveClicked)
                            }
                        },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Simpan note",
                            )
                        }
                    }
                },
            )
        },
        ) { innerPadding ->
        if (state.isLoadingNote) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(
                items = state.blocks,
                key = { block -> block.id },
            ) { block ->
                when (block) {
                    is NoteEditorBlock.Text -> {
                        TextField(
                            value = block.value,
                            onValueChange = { value ->
                                onIntent(
                                    NoteEditorIntent.TextBlockChanged(
                                        blockId = block.id,
                                        value = value,
                                    ),
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        onIntent(NoteEditorIntent.TextBlockFocused(block.id))
                                    }
                                },
                            placeholder = {
                                Text("Tulis note dan tambahkan foto atau lokasi")
                            },
                            minLines = 2,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                            ),
                        )
                    }

                    is NoteEditorBlock.Image -> {
                        val visibilityState = remember(block.id) {
                            MutableTransitionState(false).apply {
                                targetState = true
                            }
                        }

                        AnimatedVisibility(
                            visibleState = visibilityState,
                            enter = fadeIn(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                ),
                            ) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                ),
                            ),
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        IconButton(
                                            onClick = {
                                                onIntent(
                                                    NoteEditorIntent.RemoveImageClicked(
                                                        blockId = block.id,
                                                    ),
                                                )
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Hapus foto",
                                            )
                                        }
                                    }
                                    AsyncImage(
                                        model = block.uri,
                                        contentDescription = "Foto note",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp),
                                    )
                                    Text(
                                        text = "Foto tersisip",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    is NoteEditorBlock.Location -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Column {
                                    Text(
                                        text = block.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Text(
                                        text = formatCoordinate(block.latitude, block.longitude),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCoordinate(
    latitude: Double,
    longitude: Double,
): String {
    return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
}

@Preview(showBackground = true)
@Composable
private fun NoteEditorScreenPreview() {
    NotedTheme {
        NoteEditorScreen(
            state = NoteEditorPreviewData.idle,
            onIntent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteEditorScreenSavingPreview() {
    NotedTheme {
        NoteEditorScreen(
            state = NoteEditorPreviewData.saving,
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
