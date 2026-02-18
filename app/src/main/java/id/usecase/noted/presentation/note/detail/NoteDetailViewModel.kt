package id.usecase.noted.presentation.note.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.usecase.noted.data.sync.NoteEngagementRepository
import id.usecase.noted.domain.NoteHistoryRepository
import id.usecase.noted.domain.NoteRepository
import id.usecase.noted.data.sync.ExploreRepository
import id.usecase.noted.data.sync.SessionStore
import id.usecase.noted.shared.note.NoteCommentDto
import id.usecase.noted.shared.note.NoteEngagementDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteDetailViewModel(
    private val exploreRepository: ExploreRepository,
    private val noteRepository: NoteRepository,
    private val noteHistoryRepository: NoteHistoryRepository,
    private val noteEngagementRepository: NoteEngagementRepository,
    private val sessionStore: SessionStore? = null,
    private val context: Context? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(NoteDetailState())
    val state: StateFlow<NoteDetailState> = _state.asStateFlow()

    private val _effect = Channel<NoteDetailEffect>(Channel.BUFFERED)
    val effect: Flow<NoteDetailEffect> = _effect.receiveAsFlow()

    private var currentNoteId: String? = null
    private var loadJob: Job? = null
    private var loveJob: Job? = null
    private var engagementJob: Job? = null
    private var commentsJob: Job? = null
    private var submitCommentJob: Job? = null

    fun loadNote(noteId: String) {
        currentNoteId = noteId
        loadJob?.cancel()
        loveJob?.cancel()
        engagementJob?.cancel()
        commentsJob?.cancel()
        submitCommentJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    note = null,
                    errorMessage = null,
                    engagement = NoteEngagementUi(),
                    comments = emptyList(),
                    isCommentsLoading = false,
                    isSendingComment = false,
                    commentInput = "",
                    nextBeforeEpochMillis = null,
                    hasMoreComments = false,
                )
            }

            exploreRepository.getNoteById(noteId)
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            note = null,
                            errorMessage = error.message ?: "Gagal memuat note",
                            engagement = NoteEngagementUi(),
                            comments = emptyList(),
                            isCommentsLoading = false,
                            isSendingComment = false,
                            commentInput = "",
                            nextBeforeEpochMillis = null,
                            hasMoreComments = false,
                        )
                    }
                }
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
                                    engagement = NoteEngagementUi(),
                                    comments = emptyList(),
                                    isCommentsLoading = false,
                                    isSendingComment = false,
                                    commentInput = "",
                                    nextBeforeEpochMillis = null,
                                    hasMoreComments = false,
                                )
                            }
                            recordToHistory(note)
                            engagementJob = viewModelScope.launch {
                                refreshEngagement(noteId = note.id)
                            }
                            commentsJob = viewModelScope.launch {
                                loadComments(noteId = note.id, reset = true)
                            }
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    note = null,
                                    errorMessage = error.message ?: "Gagal memuat note",
                                    engagement = NoteEngagementUi(),
                                    comments = emptyList(),
                                    isCommentsLoading = false,
                                    isSendingComment = false,
                                    commentInput = "",
                                    nextBeforeEpochMillis = null,
                                    hasMoreComments = false,
                                )
                            }
                        }
                }
        }
    }

    fun onIntent(intent: NoteDetailIntent) {
        when (intent) {
            is NoteDetailIntent.NoteOpened -> loadNote(intent.noteId)
            NoteDetailIntent.ForkClicked -> forkNote()
            NoteDetailIntent.SaveClicked -> saveNote()
            NoteDetailIntent.CopyContentClicked -> copyContent()
            NoteDetailIntent.RetryClicked -> currentNoteId?.let { loadNote(it) }
            NoteDetailIntent.LoveClicked -> toggleLove()
            NoteDetailIntent.CommentsClicked -> _effect.trySend(NoteDetailEffect.ScrollToComments)
            is NoteDetailIntent.CommentInputChanged -> _state.update { it.copy(commentInput = intent.value) }
            NoteDetailIntent.SubmitCommentClicked -> submitComment()
            NoteDetailIntent.LoadMoreComments -> loadMoreComments()
            NoteDetailIntent.RefreshEngagement -> {
                _state.value.note?.let { note ->
                    engagementJob?.cancel()
                    engagementJob = viewModelScope.launch {
                        refreshEngagement(note.id)
                    }
                }
            }
        }
    }

    private fun toggleLove() {
        val note = _state.value.note ?: return
        if (loveJob?.isActive == true) {
            return
        }

        val previousEngagement = _state.value.engagement
        val optimisticEngagement = if (previousEngagement.hasLovedByMe) {
            previousEngagement.copy(
                hasLovedByMe = false,
                loveCount = (previousEngagement.loveCount - 1).coerceAtLeast(0),
            )
        } else {
            previousEngagement.copy(
                hasLovedByMe = true,
                loveCount = previousEngagement.loveCount + 1,
            )
        }

        _state.update { it.copy(engagement = optimisticEngagement) }

        loveJob = viewModelScope.launch {
            val result = if (previousEngagement.hasLovedByMe) {
                noteEngagementRepository.removeLove(noteId = note.id)
            } else {
                noteEngagementRepository.addLove(noteId = note.id)
            }

            result
                .onSuccess { engagement ->
                    if (currentNoteId != note.id) {
                        return@onSuccess
                    }
                    _state.update {
                        it.copy(engagement = engagement.toUi())
                    }
                }
                .onFailure { error ->
                    if (currentNoteId != note.id) {
                        return@onFailure
                    }
                    _state.update {
                        it.copy(engagement = previousEngagement)
                    }
                    _effect.trySend(
                        NoteDetailEffect.ShowMessage(
                            error.message ?: "Gagal memperbarui reaksi",
                        ),
                    )
                }
        }
    }

    private fun submitComment() {
        val note = _state.value.note ?: return
        if (_state.value.isSendingComment || submitCommentJob?.isActive == true) {
            return
        }

        val trimmedInput = _state.value.commentInput.trim()
        if (trimmedInput.isBlank()) {
            _effect.trySend(NoteDetailEffect.ShowMessage("Komentar tidak boleh kosong"))
            return
        }
        if (trimmedInput.length > COMMENT_MAX_LENGTH) {
            _effect.trySend(NoteDetailEffect.ShowMessage("Komentar maksimal $COMMENT_MAX_LENGTH karakter"))
            return
        }

        submitCommentJob = viewModelScope.launch {
            _state.update { it.copy(isSendingComment = true) }
            noteEngagementRepository.addComment(noteId = note.id, content = trimmedInput)
                .onSuccess { comment ->
                    if (currentNoteId != note.id) {
                        return@onSuccess
                    }
                    _state.update {
                        it.copy(
                            isSendingComment = false,
                            commentInput = "",
                            comments = listOf(comment.toUi()) + it.comments,
                            engagement = it.engagement.copy(commentCount = it.engagement.commentCount + 1),
                        )
                    }
                }
                .onFailure { error ->
                    if (currentNoteId != note.id) {
                        return@onFailure
                    }
                    _state.update { it.copy(isSendingComment = false) }
                    _effect.trySend(
                        NoteDetailEffect.ShowMessage(
                            error.message ?: "Gagal mengirim komentar",
                        ),
                    )
                }
        }
    }

    private fun loadMoreComments() {
        val noteId = _state.value.note?.id ?: return
        if (_state.value.isCommentsLoading || !_state.value.hasMoreComments) {
            return
        }

        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            loadComments(
                noteId = noteId,
                beforeEpochMillis = _state.value.nextBeforeEpochMillis,
                reset = false,
            )
        }
    }

    private suspend fun refreshEngagement(noteId: String) {
        if (currentNoteId != noteId) {
            return
        }

        noteEngagementRepository.getEngagement(noteId = noteId)
            .onSuccess { engagement ->
                if (currentNoteId != noteId) {
                    return@onSuccess
                }
                _state.update { it.copy(engagement = engagement.toUi()) }
            }
            .onFailure { error ->
                if (currentNoteId != noteId) {
                    return@onFailure
                }
                _effect.trySend(
                    NoteDetailEffect.ShowMessage(
                        error.message ?: "Gagal memuat engagement",
                    ),
                )
            }
    }

    private suspend fun loadComments(
        noteId: String,
        beforeEpochMillis: Long? = null,
        reset: Boolean,
    ) {
        if (currentNoteId != noteId) {
            return
        }

        _state.update { state ->
            state.copy(
                isCommentsLoading = true,
                comments = if (reset) emptyList() else state.comments,
                nextBeforeEpochMillis = if (reset) null else state.nextBeforeEpochMillis,
                hasMoreComments = if (reset) false else state.hasMoreComments,
            )
        }

        noteEngagementRepository.getComments(
            noteId = noteId,
            limit = COMMENTS_PAGE_SIZE,
            beforeEpochMillis = beforeEpochMillis,
        ).onSuccess { page ->
            if (currentNoteId != noteId) {
                return@onSuccess
            }
            _state.update { state ->
                val mergedComments = if (reset) {
                    page.items.map(NoteCommentDto::toUi)
                } else {
                    state.comments + page.items.map(NoteCommentDto::toUi)
                }
                state.copy(
                    isCommentsLoading = false,
                    comments = mergedComments,
                    nextBeforeEpochMillis = page.nextBeforeEpochMillis,
                    hasMoreComments = page.nextBeforeEpochMillis != null,
                )
            }
        }.onFailure { error ->
            if (currentNoteId != noteId) {
                return@onFailure
            }
            _state.update { it.copy(isCommentsLoading = false) }
            _effect.trySend(
                NoteDetailEffect.ShowMessage(
                    error.message ?: "Gagal memuat komentar",
                ),
            )
        }
    }

    private fun forkNote() {
        val note = _state.value.note ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val content = buildString {
                    appendLine(note.content)
                    appendLine()
                    appendLine("— Forked from note ${note.id}")
                }
                val newNote = noteRepository.addNote(content)
                _state.update { it.copy(isLoading = false) }
                _effect.trySend(NoteDetailEffect.ShowMessage("Note berhasil di-fork"))
                _effect.trySend(NoteDetailEffect.NavigateToEditor(newNote.id))
            } catch (error: Throwable) {
                if (error is CancellationException) {
                    throw error
                }
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
            try {
                val newNote = noteRepository.addNote(note.content)
                _state.update { it.copy(isLoading = false) }
                _effect.trySend(NoteDetailEffect.ShowMessage("Note berhasil disimpan"))
                _effect.trySend(NoteDetailEffect.NavigateToEditor(newNote.id))
            } catch (error: Throwable) {
                if (error is CancellationException) {
                    throw error
                }
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
        val safeContext = context ?: return

        val clipboardManager = safeContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Note content", note.content)
        clipboardManager.setPrimaryClip(clipData)

        _effect.trySend(NoteDetailEffect.ShowMessage("Konten disalin ke clipboard"))
    }

    private suspend fun recordToHistory(note: ExternalNote) {
        try {
            val session = sessionStore?.currentSession() ?: return
            val currentUserId = session.userId

            // Only record history for notes from other users (not own notes)
            if (currentUserId != null && currentUserId != note.ownerUserId) {
                noteHistoryRepository.addToHistory(
                    noteId = note.id,
                    ownerUserId = note.ownerUserId,
                    content = note.content,
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            Log.w(TAG, "Failed to record note history for noteId=${note.id}", error)
        }
    }

    private companion object {
        const val TAG: String = "NoteDetailViewModel"
        const val COMMENTS_PAGE_SIZE: Int = 20
        const val COMMENT_MAX_LENGTH: Int = 500
    }
}

private fun NoteEngagementDto.toUi(): NoteEngagementUi {
    return NoteEngagementUi(
        loveCount = loveCount,
        hasLovedByMe = hasLovedByMe,
        commentCount = commentCount,
    )
}

private fun NoteCommentDto.toUi(): NoteCommentUi {
    return NoteCommentUi(
        id = id,
        authorUserId = authorUserId,
        authorUsername = authorUsername,
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}
