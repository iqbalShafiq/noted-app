package id.usecase.noted.feature.note.presentation.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.usecase.noted.feature.note.data.NoteRepository
import id.usecase.noted.feature.note.domain.NoteContentBlock
import id.usecase.noted.feature.note.domain.NoteContentCodec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class NoteEditorViewModel(
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(NoteEditorState())
    val state: StateFlow<NoteEditorState> = _state.asStateFlow()

    private val _effect = Channel<NoteEditorEffect>(Channel.BUFFERED)
    val effect: Flow<NoteEditorEffect> = _effect.receiveAsFlow()

    private var nextBlockId: Long = 2L

    fun onIntent(intent: NoteEditorIntent) {
        when (intent) {
            is NoteEditorIntent.EditorOpened -> openEditor(intent.noteId)
            is NoteEditorIntent.TextBlockChanged -> updateTextBlock(intent.blockId, intent.value)
            is NoteEditorIntent.TextBlockFocused -> updateFocusedTextBlock(intent.blockId)
            NoteEditorIntent.InsertPhotoFromCameraClicked -> {
                sendEffect(NoteEditorEffect.NavigateToCamera)
            }

            NoteEditorIntent.InsertPhotoFromGalleryClicked -> {
                sendEffect(NoteEditorEffect.LaunchPhotoPicker)
            }

            NoteEditorIntent.TagLocationClicked -> {
                val lastLocationBlock = state.value.blocks
                    .filterIsInstance<NoteEditorBlock.Location>()
                    .lastOrNull()
                sendEffect(
                    NoteEditorEffect.NavigateToLocationPicker(
                        initialLocation = lastLocationBlock?.toEditorLocation(),
                    ),
                )
            }

            is NoteEditorIntent.PhotoPicked -> insertPhoto(intent.uri)
            is NoteEditorIntent.RemoveImageClicked -> removeImage(intent.blockId)
            is NoteEditorIntent.LocationTagged -> {
                insertLocation(
                    latitude = intent.latitude,
                    longitude = intent.longitude,
                    label = intent.label,
                )
            }

            NoteEditorIntent.SaveClicked -> saveNote()
        }
    }

    private fun openEditor(noteId: Long?) {
        if (noteId == null) {
            setFreshEditorState()
            return
        }

        _state.update { currentState ->
            currentState.copy(
                isLoadingNote = true,
                isSaving = false,
                editingNoteId = noteId,
            )
        }

        viewModelScope.launch {
            val note = noteRepository.getNoteById(noteId)
            if (note == null) {
                setFreshEditorState()
                sendEffect(NoteEditorEffect.ShowMessage("Note tidak ditemukan"))
                return@launch
            }

            val blocks = decodeBlocks(content = note.content)
            val focusedBlockId = blocks
                .filterIsInstance<NoteEditorBlock.Text>()
                .firstOrNull()
                ?.id

            _state.update { currentState ->
                currentState.copy(
                    blocks = blocks,
                    focusedTextBlockId = focusedBlockId,
                    editingNoteId = note.id,
                    isLoadingNote = false,
                    isSaving = false,
                )
            }
        }
    }

    private fun setFreshEditorState() {
        nextBlockId = 1L
        val initialTextBlock = createEmptyTextBlock(generateBlockId())
        _state.value = NoteEditorState(
            blocks = listOf(initialTextBlock),
            focusedTextBlockId = initialTextBlock.id,
            editingNoteId = null,
            isLoadingNote = false,
            isSaving = false,
        )
    }

    private fun decodeBlocks(content: String): List<NoteEditorBlock> {
        nextBlockId = 1L
        val decoded = NoteContentCodec.decode(content)
        val mappedBlocks = decoded.map { block ->
            when (block) {
                is NoteContentBlock.Text -> {
                    NoteEditorBlock.Text(
                        id = generateBlockId(),
                        value = TextFieldValue(
                            text = block.value,
                            selection = TextRange(block.value.length),
                        ),
                    )
                }

                is NoteContentBlock.Image -> {
                    NoteEditorBlock.Image(
                        id = generateBlockId(),
                        uri = block.uri,
                    )
                }

                is NoteContentBlock.Location -> {
                    NoteEditorBlock.Location(
                        id = generateBlockId(),
                        latitude = block.latitude,
                        longitude = block.longitude,
                        label = block.label,
                    )
                }
            }
        }

        return ensureContainsTextBlock(mappedBlocks)
    }

    private fun updateTextBlock(
        blockId: Long,
        value: TextFieldValue,
    ) {
        _state.update { currentState ->
            currentState.copy(
                blocks = currentState.blocks.map { block ->
                    if (block is NoteEditorBlock.Text && block.id == blockId) {
                        block.copy(value = value)
                    } else {
                        block
                    }
                },
            )
        }
    }

    private fun updateFocusedTextBlock(blockId: Long) {
        _state.update { currentState ->
            currentState.copy(focusedTextBlockId = blockId)
        }
    }

    private fun insertPhoto(uri: String) {
        if (uri.isBlank()) {
            return
        }

        insertContentBlock { blockId ->
            NoteEditorBlock.Image(
                id = blockId,
                uri = uri,
            )
        }
    }

    private fun insertLocation(
        latitude: Double,
        longitude: Double,
        label: String,
    ) {
        val normalizedLabel = label.trim().ifEmpty { formatCoordinate(latitude, longitude) }
        insertContentBlock { blockId ->
            NoteEditorBlock.Location(
                id = blockId,
                latitude = latitude,
                longitude = longitude,
                label = normalizedLabel,
            )
        }
    }

    private fun removeImage(blockId: Long) {
        _state.update { currentState ->
            val updatedBlocks = currentState.blocks.filterNot { block ->
                block is NoteEditorBlock.Image && block.id == blockId
            }
            if (updatedBlocks.size == currentState.blocks.size) {
                return@update currentState
            }

            val normalizedBlocks = ensureContainsTextBlock(updatedBlocks)
            val stillFocused = currentState.focusedTextBlockId
                ?.takeIf { focusedId -> normalizedBlocks.any { block -> block.id == focusedId } }
            val fallbackFocus = normalizedBlocks
                .filterIsInstance<NoteEditorBlock.Text>()
                .lastOrNull()
                ?.id

            currentState.copy(
                blocks = normalizedBlocks,
                focusedTextBlockId = stillFocused ?: fallbackFocus,
            )
        }
    }

    private fun insertContentBlock(
        createBlock: (Long) -> NoteEditorBlock,
    ) {
        _state.update { currentState ->
            val focusedId = currentState.focusedTextBlockId
                ?: currentState.blocks.filterIsInstance<NoteEditorBlock.Text>().lastOrNull()?.id
            val insertedBlock = createBlock(generateBlockId())

            if (focusedId == null) {
                val trailingTextBlock = createEmptyTextBlock(generateBlockId())
                return@update currentState.copy(
                    blocks = currentState.blocks + insertedBlock + trailingTextBlock,
                    focusedTextBlockId = trailingTextBlock.id,
                )
            }

            val targetIndex = currentState.blocks.indexOfFirst { block ->
                block is NoteEditorBlock.Text && block.id == focusedId
            }

            if (targetIndex < 0) {
                val trailingTextBlock = createEmptyTextBlock(generateBlockId())
                return@update currentState.copy(
                    blocks = currentState.blocks + insertedBlock + trailingTextBlock,
                    focusedTextBlockId = trailingTextBlock.id,
                )
            }

            val targetTextBlock = currentState.blocks[targetIndex] as NoteEditorBlock.Text
            val textValue = targetTextBlock.value
            val rawSelectionStart = minOf(textValue.selection.start, textValue.selection.end)
            val rawSelectionEnd = maxOf(textValue.selection.start, textValue.selection.end)
            val selectionStart = rawSelectionStart.coerceIn(0, textValue.text.length)
            val selectionEnd = rawSelectionEnd.coerceIn(0, textValue.text.length)
            val leadingText = textValue.text.substring(0, selectionStart)
            val trailingText = textValue.text.substring(selectionEnd)

            val replacement = mutableListOf<NoteEditorBlock>()
            if (leadingText.isNotEmpty()) {
                replacement += NoteEditorBlock.Text(
                    id = targetTextBlock.id,
                    value = TextFieldValue(
                        text = leadingText,
                        selection = TextRange(leadingText.length),
                    ),
                )
            }

            replacement += insertedBlock
            val trailingTextBlock = NoteEditorBlock.Text(
                id = generateBlockId(),
                value = TextFieldValue(
                    text = trailingText,
                    selection = TextRange(0),
                ),
            )
            replacement += trailingTextBlock

            val updatedBlocks = currentState.blocks.toMutableList().apply {
                removeAt(targetIndex)
                addAll(targetIndex, replacement)
            }

            currentState.copy(
                blocks = updatedBlocks,
                focusedTextBlockId = trailingTextBlock.id,
            )
        }
    }

    private fun ensureContainsTextBlock(blocks: List<NoteEditorBlock>): List<NoteEditorBlock> {
        if (blocks.any { block -> block is NoteEditorBlock.Text }) {
            return blocks
        }

        return blocks + createEmptyTextBlock(generateBlockId())
    }

    private fun saveNote() {
        if (state.value.isSaving) {
            return
        }

        val currentState = state.value
        val blocks = currentState.blocks
        val editingNoteId = currentState.editingNoteId
        val hasText = blocks.any { block ->
            block is NoteEditorBlock.Text && block.value.text.trim().isNotEmpty()
        }
        val hasImage = blocks.any { block -> block is NoteEditorBlock.Image }
        val hasLocation = blocks.any { block -> block is NoteEditorBlock.Location }

        if (!hasText && !hasImage && !hasLocation) {
            sendEffect(NoteEditorEffect.ShowMessage("Isi note tidak boleh kosong"))
            return
        }

        val content = NoteContentCodec.encode(
            blocks = blocks.map { block ->
                when (block) {
                    is NoteEditorBlock.Text -> NoteContentBlock.Text(block.value.text)
                    is NoteEditorBlock.Image -> NoteContentBlock.Image(block.uri)
                    is NoteEditorBlock.Location -> {
                        NoteContentBlock.Location(
                            latitude = block.latitude,
                            longitude = block.longitude,
                            label = block.label,
                        )
                    }
                }
            },
        )

        _state.update { currentState ->
            currentState.copy(isSaving = true)
        }

        viewModelScope.launch {
            runCatching {
                if (editingNoteId == null) {
                    noteRepository.addNote(content)
                } else {
                    noteRepository.updateNote(editingNoteId, content)
                        ?: error("Note tidak ditemukan")
                }
            }.onSuccess {
                setFreshEditorState()
                val successMessage =
                    if (editingNoteId == null) "Note berhasil disimpan" else "Note berhasil diperbarui"
                sendEffect(NoteEditorEffect.ShowMessage(successMessage))
                sendEffect(NoteEditorEffect.NavigateToList)
            }.onFailure { error ->
                _state.update { currentState ->
                    currentState.copy(isSaving = false)
                }
                val fallbackMessage =
                    if (editingNoteId == null) "Gagal menyimpan note" else "Gagal memperbarui note"
                sendEffect(
                    NoteEditorEffect.ShowMessage(
                        error.message ?: fallbackMessage,
                    ),
                )
            }
        }
    }

    private fun sendEffect(effect: NoteEditorEffect) {
        _effect.trySend(effect)
    }

    private fun generateBlockId(): Long {
        val id = nextBlockId
        nextBlockId += 1
        return id
    }

    private fun formatCoordinate(
        latitude: Double,
        longitude: Double,
    ): String {
        return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
    }

    private fun NoteEditorBlock.Location.toEditorLocation(): NoteEditorLocation {
        return NoteEditorLocation(
            latitude = latitude,
            longitude = longitude,
            label = label,
        )
    }

    companion object {
        fun factory(noteRepository: NoteRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NoteEditorViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return NoteEditorViewModel(noteRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
