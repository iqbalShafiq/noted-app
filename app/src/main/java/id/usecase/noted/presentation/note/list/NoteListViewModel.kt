package id.usecase.noted.presentation.note.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.usecase.noted.data.NoteRepository
import id.usecase.noted.data.sync.NoteSyncCoordinator
import id.usecase.noted.domain.Note
import id.usecase.noted.domain.NoteContentCodec
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteListViewModel(
    private val noteRepository: NoteRepository,
    private val noteSyncCoordinator: NoteSyncCoordinator,
) : ViewModel() {
    private val _state = MutableStateFlow(NoteListState())
    val state: StateFlow<NoteListState> = _state.asStateFlow()

    private val _effect = Channel<NoteListEffect>(Channel.BUFFERED)
    val effect: Flow<NoteListEffect> = _effect.receiveAsFlow()

    private var observeJob: Job? = null
    private var syncObserveJob: Job? = null

    init {
        startObserveNotes()
        startObserveSyncStatus()
    }

    fun onIntent(intent: NoteListIntent) {
        when (intent) {
            NoteListIntent.AddNoteClicked -> {
                _effect.trySend(NoteListEffect.NavigateToEditor(noteId = null))
            }

            is NoteListIntent.NoteClicked -> {
                _effect.trySend(NoteListEffect.NavigateToEditor(noteId = intent.noteId))
            }

            is NoteListIntent.NoteDeleteClicked -> deleteNote(intent.noteId)

            NoteListIntent.RetryObserve -> startObserveNotes()

            is NoteListIntent.SearchQueryChanged -> {
                _state.update { currentState ->
                    currentState.copy(searchQuery = intent.query)
                }
            }

            NoteListIntent.SearchClicked -> {
                // Search is handled reactively via state update
                // UI can use filteredNotes from state
            }

            NoteListIntent.SyncClicked -> {
                _effect.trySend(NoteListEffect.NavigateToSync)
            }

            NoteListIntent.AccountClicked -> {
                _effect.trySend(NoteListEffect.NavigateToAccount)
            }
        }
    }

    private fun startObserveNotes() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            noteRepository.observeNotes()
                .map { notes -> notes.map(Note::toListItemUi) }
                .catch { error ->
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Gagal memuat daftar note",
                        )
                    }
                    _effect.trySend(
                        NoteListEffect.ShowMessage(
                            error.message ?: "Gagal memuat daftar note",
                        ),
                    )
                }
                .collect { notes ->
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = null,
                            notes = notes,
                        )
                    }
                }
        }
    }

    private fun startObserveSyncStatus() {
        syncObserveJob?.cancel()
        syncObserveJob = viewModelScope.launch {
            noteSyncCoordinator.syncStatus.collect { syncStatus ->
                _state.update { currentState ->
                    currentState.copy(
                        syncStatus = syncStatus,
                    )
                }
            }
        }
    }

    private fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            runCatching {
                noteRepository.deleteNote(noteId)
            }.onSuccess { deleted ->
                if (deleted) {
                    _effect.trySend(NoteListEffect.ShowMessage("Note dihapus"))
                } else {
                    _effect.trySend(NoteListEffect.ShowMessage("Note tidak ditemukan"))
                }
            }.onFailure { error ->
                _effect.trySend(
                    NoteListEffect.ShowMessage(
                        error.message ?: "Gagal menghapus note",
                    ),
                )
            }
        }
    }

}

private fun Note.toListItemUi(): NoteListItemUi {
    return NoteListItemUi(
        id = id,
        content = NoteContentCodec.toListPreview(content),
        createdAt = createdAt,
    )
}
