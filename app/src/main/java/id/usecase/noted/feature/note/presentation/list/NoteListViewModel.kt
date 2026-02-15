package id.usecase.noted.feature.note.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.usecase.noted.feature.note.data.NoteRepository
import id.usecase.noted.feature.note.data.sync.NoteSyncCoordinator
import id.usecase.noted.feature.note.domain.Note
import id.usecase.noted.feature.note.domain.NoteContentCodec
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

            is NoteListIntent.LoginInputChanged -> {
                _state.update { currentState ->
                    currentState.copy(loginInput = intent.value)
                }
            }

            is NoteListIntent.PasswordInputChanged -> {
                _state.update { currentState ->
                    currentState.copy(passwordInput = intent.value)
                }
            }

            NoteListIntent.LoginSubmitClicked -> signIn()
            NoteListIntent.RegisterSubmitClicked -> register()
            NoteListIntent.LogoutClicked -> signOut()
            NoteListIntent.SyncNowClicked -> syncNow()
            NoteListIntent.UploadNowClicked -> uploadNow()
            NoteListIntent.ImportNowClicked -> importNow()
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
                    val normalizedInput = if (currentState.loginInput.isBlank() && !syncStatus.userId.isNullOrBlank()) {
                        syncStatus.userId
                    } else {
                        currentState.loginInput
                    }
                    currentState.copy(
                        syncStatus = syncStatus,
                        loginInput = normalizedInput,
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

    private fun register() {
        val username = state.value.loginInput.trim()
        val password = state.value.passwordInput
        if (username.isBlank()) {
            _effect.trySend(NoteListEffect.ShowMessage("Username wajib diisi"))
            return
        }
        if (password.length < 8) {
            _effect.trySend(NoteListEffect.ShowMessage("Password minimal 8 karakter"))
            return
        }

        viewModelScope.launch {
            runCatching {
                noteSyncCoordinator.register(username = username, password = password)
            }.onSuccess {
                _state.update { currentState ->
                    currentState.copy(passwordInput = "")
                }
                _effect.trySend(NoteListEffect.ShowMessage("Registrasi berhasil sebagai $username"))
            }.onFailure { error ->
                _effect.trySend(
                    NoteListEffect.ShowMessage(
                        error.message ?: "Registrasi gagal",
                    ),
                )
            }
        }
    }

    private fun signIn() {
        val username = state.value.loginInput.trim()
        val password = state.value.passwordInput
        if (username.isBlank()) {
            _effect.trySend(NoteListEffect.ShowMessage("Masukkan username terlebih dahulu"))
            return
        }
        if (password.length < 8) {
            _effect.trySend(NoteListEffect.ShowMessage("Password minimal 8 karakter"))
            return
        }

        viewModelScope.launch {
            runCatching {
                noteSyncCoordinator.login(username = username, password = password)
            }.onSuccess {
                _state.update { currentState ->
                    currentState.copy(passwordInput = "")
                }
                _effect.trySend(NoteListEffect.ShowMessage("Login sebagai $username berhasil"))
            }.onFailure { error ->
                _effect.trySend(
                    NoteListEffect.ShowMessage(
                        error.message ?: "Login gagal",
                    ),
                )
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            runCatching {
                noteSyncCoordinator.signOut()
            }.onSuccess {
                _state.update { currentState ->
                    currentState.copy(loginInput = "", passwordInput = "")
                }
                _effect.trySend(NoteListEffect.ShowMessage("Logout berhasil"))
            }.onFailure { error ->
                _effect.trySend(
                    NoteListEffect.ShowMessage(
                        error.message ?: "Logout gagal",
                    ),
                )
            }
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            runCatching {
                noteSyncCoordinator.syncNow()
            }.onFailure { error ->
                _effect.trySend(
                    NoteListEffect.ShowMessage(
                        error.message ?: "Sinkronisasi gagal",
                    ),
                )
            }
        }
    }

    private fun uploadNow() {
        viewModelScope.launch {
            runCatching {
                noteSyncCoordinator.uploadPendingNow()
            }.onFailure { error ->
                _effect.trySend(
                    NoteListEffect.ShowMessage(
                        error.message ?: "Upload manual gagal",
                    ),
                )
            }
        }
    }

    private fun importNow() {
        viewModelScope.launch {
            runCatching {
                noteSyncCoordinator.importNow()
            }.onFailure { error ->
                _effect.trySend(
                    NoteListEffect.ShowMessage(
                        error.message ?: "Import dari server gagal",
                    ),
                )
            }
        }
    }

    companion object {
        fun factory(
            noteRepository: NoteRepository,
            noteSyncCoordinator: NoteSyncCoordinator,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NoteListViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return NoteListViewModel(
                            noteRepository = noteRepository,
                            noteSyncCoordinator = noteSyncCoordinator,
                        ) as T
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
