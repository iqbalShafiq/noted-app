package id.usecase.noted.feature.note.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.usecase.noted.feature.note.data.NoteRepository
import id.usecase.noted.feature.note.domain.NoteContentCodec
import id.usecase.noted.feature.note.domain.Note
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteListViewModel(
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(NoteListState())
    val state: StateFlow<NoteListState> = _state.asStateFlow()

    private val _effect = Channel<NoteListEffect>(Channel.BUFFERED)
    val effect: Flow<NoteListEffect> = _effect.receiveAsFlow()

    private var observeJob: Job? = null

    init {
        startObserveNotes()
    }

    fun onIntent(intent: NoteListIntent) {
        when (intent) {
            NoteListIntent.AddNoteClicked -> {
                _effect.trySend(NoteListEffect.NavigateToEditor(noteId = null))
            }

            is NoteListIntent.NoteClicked -> {
                _effect.trySend(NoteListEffect.NavigateToEditor(noteId = intent.noteId))
            }

            NoteListIntent.RetryObserve -> startObserveNotes()
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

    companion object {
        fun factory(noteRepository: NoteRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NoteListViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return NoteListViewModel(noteRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
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
