package id.usecase.noted.presentation.note.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.usecase.noted.data.sync.ExploreRepository
import id.usecase.noted.shared.note.NoteDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val exploreRepository: ExploreRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    private val _effect = Channel<ExploreEffect>(Channel.BUFFERED)
    val effect: Flow<ExploreEffect> = _effect.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadExploreNotes()
    }

    fun onIntent(intent: ExploreIntent) {
        when (intent) {
            ExploreIntent.LoadExploreNotes -> loadExploreNotes()
            ExploreIntent.RefreshExploreNotes -> loadExploreNotes(isRefresh = true)
            ExploreIntent.NavigateBackClicked -> _effect.trySend(ExploreEffect.NavigateBack)
            is ExploreIntent.SearchQueryChanged -> _state.update { it.copy(searchQuery = intent.query) }
            ExploreIntent.SearchClicked -> _state.update { it.copy(isSearchExpanded = true) }
            ExploreIntent.DismissSearch -> _state.update { it.copy(isSearchExpanded = false, searchQuery = "") }
            ExploreIntent.ClearSearchHistory -> _state.update { it.copy(searchHistory = emptyList()) }
            is ExploreIntent.NoteClicked -> _effect.trySend(ExploreEffect.NavigateToNoteDetail(noteId = intent.noteId))
        }
    }

    private fun loadExploreNotes(isRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!isRefresh) {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
            }

            exploreRepository.exploreNotes(limit = 50)
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Gagal memuat note",
                        )
                    }
                    _effect.trySend(
                        ExploreEffect.ShowMessage(
                            error.message ?: "Gagal memuat note",
                        ),
                    )
                }
                .collect { result ->
                    result
                        .onSuccess { notes ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = null,
                                    notes = notes.map(NoteDto::toExploreNoteUi),
                                )
                            }
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Gagal memuat note",
                                )
                            }
                            _effect.trySend(
                                ExploreEffect.ShowMessage(
                                    error.message ?: "Gagal memuat note",
                                ),
                            )
                        }
                }
        }
    }
}

private fun NoteDto.toExploreNoteUi(): ExploreNoteUi {
    return ExploreNoteUi(
        id = id,
        content = content,
        ownerUserId = ownerUserId,
        createdAt = createdAtEpochMillis,
    )
}
