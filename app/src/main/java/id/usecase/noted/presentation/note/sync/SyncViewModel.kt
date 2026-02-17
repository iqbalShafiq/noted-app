package id.usecase.noted.presentation.note.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.usecase.noted.domain.NoteRepository
import id.usecase.noted.data.sync.LocalSyncStatus
import id.usecase.noted.data.local.NoteDao
import id.usecase.noted.data.sync.NoteSyncCoordinator
import id.usecase.noted.data.sync.NoteSyncStatus
import id.usecase.noted.domain.NoteContentCodec
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncViewModel(
    private val noteRepository: NoteRepository,
    private val noteSyncCoordinator: NoteSyncCoordinator,
    private val noteDao: NoteDao,
) : ViewModel() {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val _effect = Channel<SyncEffect>(Channel.BUFFERED)
    val effect: Flow<SyncEffect> = _effect.receiveAsFlow()

    private var syncObserveJob: Job? = null
    private var pendingNotesObserveJob: Job? = null

    init {
        startObserveSyncStatus()
        startObservePendingNotes()
    }

    fun onIntent(intent: SyncIntent) {
        when (intent) {
            SyncIntent.LoadSyncStatus -> {
                startObserveSyncStatus()
                startObservePendingNotes()
            }

            SyncIntent.SyncNowClicked -> syncNow()
            SyncIntent.UploadNowClicked -> uploadNow()
            SyncIntent.ImportNowClicked -> importNow()

            is SyncIntent.RetryNoteClicked -> retryNote(intent.noteId)

            SyncIntent.NavigateBackClicked -> {
                _effect.trySend(SyncEffect.NavigateBack)
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

    private fun startObservePendingNotes() {
        pendingNotesObserveJob?.cancel()
        pendingNotesObserveJob = viewModelScope.launch {
            combine(
                noteSyncCoordinator.session,
                noteSyncCoordinator.syncStatus,
            ) { session, syncStatus ->
                session to syncStatus
            }.collect { (session, _) ->
                val pendingNotes = if (session.userId != null) {
                    noteDao.getPendingMutations(session.userId).map { entity ->
                        PendingNoteUi(
                            id = entity.id,
                            content = NoteContentCodec.toListPreview(entity.content),
                            createdAt = entity.createdAt,
                            isFailed = entity.syncStatus == LocalSyncStatus.SYNC_ERROR.name,
                            errorMessage = entity.syncErrorMessage,
                        )
                    }
                } else {
                    emptyList()
                }

                _state.update { currentState ->
                    currentState.copy(
                        pendingNotes = pendingNotes,
                    )
                }
            }
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            runCatching {
                noteSyncCoordinator.syncNow()
            }.onSuccess {
                _effect.trySend(SyncEffect.ShowMessage("Sinkronisasi berhasil"))
            }.onFailure { error ->
                _effect.trySend(
                    SyncEffect.ShowMessage(
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
            }.onSuccess {
                _effect.trySend(SyncEffect.ShowMessage("Upload manual berhasil"))
            }.onFailure { error ->
                _effect.trySend(
                    SyncEffect.ShowMessage(
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
            }.onSuccess {
                _effect.trySend(SyncEffect.ShowMessage("Import dari server berhasil"))
            }.onFailure { error ->
                _effect.trySend(
                    SyncEffect.ShowMessage(
                        error.message ?: "Import dari server gagal",
                    ),
                )
            }
        }
    }

    private fun retryNote(noteId: Long) {
        viewModelScope.launch {
            runCatching {
                val note = noteDao.getNoteById(noteId)
                if (note != null) {
                    val currentSession = noteSyncCoordinator.session.value
                    val ownerUserId = note.ownerUserId ?: currentSession.userId

                    if (ownerUserId != null) {
                        noteDao.updateContent(
                            noteId = noteId,
                            content = note.content,
                            updatedAt = System.currentTimeMillis(),
                            ownerUserId = ownerUserId,
                            syncStatus = LocalSyncStatus.PENDING_UPSERT.name,
                        )

                        if (currentSession.userId != null) {
                            noteSyncCoordinator.syncNow()
                        }
                    }
                }
            }.onSuccess {
                _effect.trySend(SyncEffect.ShowMessage("Note di-retry untuk sinkronisasi"))
            }.onFailure { error ->
                _effect.trySend(
                    SyncEffect.ShowMessage(
                        error.message ?: "Gagal retry note",
                    ),
                )
            }
        }
    }
}
