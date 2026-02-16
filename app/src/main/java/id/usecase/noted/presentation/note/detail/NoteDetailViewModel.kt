package id.usecase.noted.presentation.note.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.usecase.noted.data.NoteHistoryRepository
import id.usecase.noted.data.NoteRepository
import id.usecase.noted.data.sync.ExploreRepository
import id.usecase.noted.data.sync.SessionStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteDetailViewModel(
    private val exploreRepository: ExploreRepository,
    private val noteRepository: NoteRepository,
    private val noteHistoryRepository: NoteHistoryRepository,
    private val sessionStore: SessionStore,
    private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(NoteDetailState())
    val state: StateFlow<NoteDetailState> = _state.asStateFlow()

    private val _effect = Channel<NoteDetailEffect>(Channel.BUFFERED)
    val effect: Flow<NoteDetailEffect> = _effect.receiveAsFlow()

    private var currentNoteId: String? = null
    private var loadJob: Job? = null

    fun loadNote(noteId: String) {
        currentNoteId = noteId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            exploreRepository.getNoteById(noteId)
                .collect { result ->
                    result
                        .onSuccess { noteDto ->
                            val note = ExternalNote(
                                id = noteDto.id,
                                ownerUserId = noteDto.ownerUserId,
                                content = noteDto.content,
                                createdAt = noteDto.createdAtEpochMillis,
                                forkedFrom = null,
                            )
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = null,
                                    note = note,
                                )
                            }
                            recordToHistory(note)
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Gagal memuat note",
                                )
                            }
                        }
                }
        }
    }

    fun onIntent(intent: NoteDetailIntent) {
        when (intent) {
            NoteDetailIntent.ForkClicked -> forkNote()
            NoteDetailIntent.SaveClicked -> saveNote()
            NoteDetailIntent.CopyContentClicked -> copyContent()
            NoteDetailIntent.RetryClicked -> currentNoteId?.let { loadNote(it) }
        }
    }

    private fun forkNote() {
        val note = _state.value.note ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                val content = buildString {
                    appendLine(note.content)
                    appendLine()
                    appendLine("— Forked from note ${note.id}")
                }
                noteRepository.addNote(content)
            }.onSuccess { newNote ->
                _state.update { it.copy(isLoading = false) }
                _effect.trySend(NoteDetailEffect.ShowMessage("Note berhasil di-fork"))
                _effect.trySend(NoteDetailEffect.NavigateToEditor(newNote.id))
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false) }
                _effect.trySend(
                    NoteDetailEffect.ShowMessage(
                        error.message ?: "Gagal fork note",
                    ),
                )
            }
        }
    }

    private fun saveNote() {
        val note = _state.value.note ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                noteRepository.addNote(note.content)
            }.onSuccess { newNote ->
                _state.update { it.copy(isLoading = false) }
                _effect.trySend(NoteDetailEffect.ShowMessage("Note berhasil disimpan"))
                _effect.trySend(NoteDetailEffect.NavigateToEditor(newNote.id))
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false) }
                _effect.trySend(
                    NoteDetailEffect.ShowMessage(
                        error.message ?: "Gagal menyimpan note",
                    ),
                )
            }
        }
    }

    private fun copyContent() {
        val note = _state.value.note ?: return

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Note content", note.content)
        clipboardManager.setPrimaryClip(clipData)

        _effect.trySend(NoteDetailEffect.ShowMessage("Konten disalin ke clipboard"))
    }

    private suspend fun recordToHistory(note: ExternalNote) {
        try {
            val session = sessionStore.currentSession()
            val currentUserId = session.userId

            // Only record history for notes from other users (not own notes)
            if (currentUserId != null && currentUserId != note.ownerUserId) {
                noteHistoryRepository.addToHistory(
                    noteId = note.id,
                    ownerUserId = note.ownerUserId,
                    content = note.content,
                )
            }
        } catch (e: Exception) {
            // Silently fail - history recording is not critical
        }
    }
}
