package id.usecase.noted.presentation.note.detail

import id.usecase.noted.data.sync.ExploreRepository
import id.usecase.noted.data.sync.NoteEngagementRepository
import id.usecase.noted.domain.Note
import id.usecase.noted.domain.NoteHistoryRepository
import id.usecase.noted.domain.NoteRepository
import id.usecase.noted.domain.NoteVisibility
import id.usecase.noted.domain.NoteHistory
import id.usecase.noted.data.sync.LocalSyncStatus
import id.usecase.noted.presentation.MainDispatcherRule
import id.usecase.noted.shared.note.NoteCommentDto
import id.usecase.noted.shared.note.NoteCommentsPageDto
import id.usecase.noted.shared.note.NoteDto
import id.usecase.noted.shared.note.NoteEngagementDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loveClickedOptimisticUpdateAndRollbackOnFailure() = runTest {
        val engagementRepository = FakeNoteEngagementRepository(
            initialEngagement = NoteEngagementDto(
                noteId = NOTE_ID,
                loveCount = 4,
                hasLovedByMe = false,
                commentCount = 1,
            ),
        )
        val addLoveDeferred = CompletableDeferred<Result<NoteEngagementDto>>()
        engagementRepository.nextAddLoveResult = addLoveDeferred
        val viewModel = createViewModel(engagementRepository = engagementRepository)

        viewModel.loadNote(NOTE_ID)
        advanceUntilIdle()

        viewModel.onIntent(NoteDetailIntent.LoveClicked)
        runCurrent()

        assertTrue(viewModel.state.value.engagement.hasLovedByMe)
        assertEquals(5, viewModel.state.value.engagement.loveCount)

        addLoveDeferred.complete(Result.failure(IllegalStateException("Gagal menambah love")))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.engagement.hasLovedByMe)
        assertEquals(4, viewModel.state.value.engagement.loveCount)
    }

    @Test
    fun commentsClickedEmitsScrollToCommentsEffectOnce() = runTest {
        val viewModel = createViewModel()
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteDetailIntent.CommentsClicked)
        advanceUntilIdle()

        assertEquals(NoteDetailEffect.ScrollToComments, firstEffect.await())
    }

    @Test
    fun submitCommentClickedRejectsBlankAndTooLongInput() = runTest {
        val engagementRepository = FakeNoteEngagementRepository()
        val viewModel = createViewModel(engagementRepository = engagementRepository)

        viewModel.loadNote(NOTE_ID)
        advanceUntilIdle()

        viewModel.onIntent(NoteDetailIntent.CommentInputChanged("   "))
        viewModel.onIntent(NoteDetailIntent.SubmitCommentClicked)
        advanceUntilIdle()

        assertEquals(0, engagementRepository.addCommentCalls)

        viewModel.onIntent(NoteDetailIntent.CommentInputChanged("a".repeat(501)))
        viewModel.onIntent(NoteDetailIntent.SubmitCommentClicked)
        advanceUntilIdle()

        assertEquals(0, engagementRepository.addCommentCalls)
    }

    @Test
    fun successfulSubmitAddsCommentIncrementsCountAndClearsInput() = runTest {
        val engagementRepository = FakeNoteEngagementRepository(
            initialEngagement = NoteEngagementDto(
                noteId = NOTE_ID,
                loveCount = 1,
                hasLovedByMe = false,
                commentCount = 1,
            ),
            initialComments = listOf(
                NoteCommentDto(
                    id = "comment-1",
                    noteId = NOTE_ID,
                    authorUserId = "user-a",
                    authorUsername = "andi",
                    content = "Komentar lama",
                    createdAtEpochMillis = 1_000L,
                ),
            ),
            addCommentResult = Result.success(
                NoteCommentDto(
                    id = "comment-2",
                    noteId = NOTE_ID,
                    authorUserId = "user-b",
                    authorUsername = "budi",
                    content = "Komentar baru",
                    createdAtEpochMillis = 2_000L,
                ),
            ),
        )
        val viewModel = createViewModel(engagementRepository = engagementRepository)

        viewModel.loadNote(NOTE_ID)
        advanceUntilIdle()

        viewModel.onIntent(NoteDetailIntent.CommentInputChanged("Komentar baru"))
        viewModel.onIntent(NoteDetailIntent.SubmitCommentClicked)
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.commentInput)
        assertEquals(2, viewModel.state.value.engagement.commentCount)
        assertEquals("comment-2", viewModel.state.value.comments.first().id)
    }

    @Test
    fun loadNoteFailureClearsStaleNoteState() = runTest {
        val exploreRepository = SwitchingExploreRepository(
            successNoteId = "note-success",
            failureNoteId = "note-failure",
        )
        val viewModel = createViewModel(exploreRepository = exploreRepository)

        viewModel.loadNote("note-success")
        advanceUntilIdle()
        assertEquals("note-success", viewModel.state.value.note?.id)

        viewModel.loadNote("note-failure")
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.note)
        assertEquals("Gagal memuat note", viewModel.state.value.errorMessage)
        assertEquals(0, viewModel.state.value.engagement.loveCount)
        assertTrue(viewModel.state.value.comments.isEmpty())
    }

    @Test
    fun loadNoteWhenRepositoryFlowThrowsSetsErrorState() = runTest {
        val viewModel = createViewModel(
            exploreRepository = ThrowingNoteByIdExploreRepository(
                message = "Login diperlukan untuk melihat note",
            ),
        )

        viewModel.loadNote(NOTE_ID)
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.note)
        assertEquals("Login diperlukan untuk melihat note", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun staleLoveResponseDoesNotOverrideNewlyOpenedNoteState() = runTest {
        val engagementRepository = FakeNoteEngagementRepository(
            initialEngagement = NoteEngagementDto(
                noteId = "note-a",
                loveCount = 0,
                hasLovedByMe = false,
                commentCount = 0,
            ),
        )
        val delayedLoveResult = CompletableDeferred<Result<NoteEngagementDto>>()
        engagementRepository.nextAddLoveResult = delayedLoveResult
        val viewModel = createViewModel(engagementRepository = engagementRepository)

        viewModel.loadNote("note-a")
        advanceUntilIdle()

        viewModel.onIntent(NoteDetailIntent.LoveClicked)
        runCurrent()

        viewModel.loadNote("note-b")
        advanceUntilIdle()

        delayedLoveResult.complete(
            Result.success(
                NoteEngagementDto(
                    noteId = "note-a",
                    loveCount = 999,
                    hasLovedByMe = true,
                    commentCount = 0,
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals("note-b", viewModel.state.value.note?.id)
        assertEquals(0, viewModel.state.value.engagement.loveCount)
    }

    private fun createViewModel(
        engagementRepository: FakeNoteEngagementRepository = FakeNoteEngagementRepository(),
        exploreRepository: ExploreRepository = FakeExploreRepository(),
    ): NoteDetailViewModel {
        return NoteDetailViewModel(
            exploreRepository = exploreRepository,
            noteRepository = FakeNoteRepository(),
            noteHistoryRepository = FakeNoteHistoryRepository(),
            noteEngagementRepository = engagementRepository,
        )
    }

    private companion object {
        const val NOTE_ID = "note-123"
    }
}

private class FakeExploreRepository : ExploreRepository {
    override fun exploreNotes(limit: Int): Flow<Result<List<NoteDto>>> = flowOf(Result.success(emptyList()))

    override fun searchExploreNotes(query: String, limit: Int): Flow<Result<List<NoteDto>>> =
        flowOf(Result.success(emptyList()))

    override fun getNoteById(noteId: String): Flow<Result<NoteDto>> {
        return flowOf(
            Result.success(
                NoteDto(
                    id = noteId,
                    ownerUserId = "owner-1",
                    content = "Isi note",
                    createdAtEpochMillis = 1_000L,
                ),
            ),
        )
    }
}

private class SwitchingExploreRepository(
    private val successNoteId: String,
    private val failureNoteId: String,
) : ExploreRepository {
    override fun exploreNotes(limit: Int): Flow<Result<List<NoteDto>>> = flowOf(Result.success(emptyList()))

    override fun searchExploreNotes(query: String, limit: Int): Flow<Result<List<NoteDto>>> =
        flowOf(Result.success(emptyList()))

    override fun getNoteById(noteId: String): Flow<Result<NoteDto>> {
        return when (noteId) {
            successNoteId -> {
                flowOf(
                    Result.success(
                        NoteDto(
                            id = successNoteId,
                            ownerUserId = "owner-success",
                            content = "Success note",
                            createdAtEpochMillis = 10_000L,
                        ),
                    ),
                )
            }

            failureNoteId -> {
                flowOf(Result.failure(IllegalStateException("Gagal memuat note")))
            }

            else -> {
                flowOf(Result.failure(IllegalArgumentException("Unknown note")))
            }
        }
    }
}

private class ThrowingNoteByIdExploreRepository(
    private val message: String,
) : ExploreRepository {
    override fun exploreNotes(limit: Int): Flow<Result<List<NoteDto>>> = flowOf(Result.success(emptyList()))

    override fun searchExploreNotes(query: String, limit: Int): Flow<Result<List<NoteDto>>> =
        flowOf(Result.success(emptyList()))

    override fun getNoteById(noteId: String): Flow<Result<NoteDto>> = flow {
        throw IllegalStateException(message)
    }
}

private class FakeNoteRepository : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> = emptyFlow()

    override suspend fun getNoteById(noteId: Long): Note? = null

    override suspend fun addNote(content: String, visibility: NoteVisibility): Note {
        return Note(
            id = 99,
            noteId = "local-99",
            content = content,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            ownerUserId = null,
            syncStatus = LocalSyncStatus.LOCAL_ONLY,
            visibility = visibility,
        )
    }

    override suspend fun updateNote(noteId: Long, content: String, visibility: NoteVisibility): Note? = null

    override suspend fun deleteNote(noteId: Long): Boolean = false
}

private class FakeNoteHistoryRepository : NoteHistoryRepository {
    override suspend fun addToHistory(noteId: String, ownerUserId: String, content: String) = Unit

    override fun getHistory(limit: Int): Flow<List<NoteHistory>> = emptyFlow()

    override suspend fun clearHistory() = Unit

    override suspend fun syncHistoryWithServer() = Unit
}

private class FakeNoteEngagementRepository(
    private val initialEngagement: NoteEngagementDto = NoteEngagementDto(
        noteId = "note-123",
        loveCount = 0,
        hasLovedByMe = false,
        commentCount = 0,
    ),
    initialComments: List<NoteCommentDto> = emptyList(),
    private val addLoveResult: Result<NoteEngagementDto> = Result.success(
        initialEngagement.copy(
            loveCount = initialEngagement.loveCount + 1,
            hasLovedByMe = true,
        ),
    ),
    private val removeLoveResult: Result<NoteEngagementDto> = Result.success(
        initialEngagement.copy(
            loveCount = (initialEngagement.loveCount - 1).coerceAtLeast(0),
            hasLovedByMe = false,
        ),
    ),
    private val commentsPageResult: Result<NoteCommentsPageDto> = Result.success(
        NoteCommentsPageDto(
            items = initialComments,
            nextBeforeEpochMillis = null,
        ),
    ),
    private val addCommentResult: Result<NoteCommentDto> = Result.success(
        NoteCommentDto(
            id = "comment-new",
            noteId = "note-123",
            authorUserId = "me",
            authorUsername = "me",
            content = "Komentar baru",
            createdAtEpochMillis = 2_000L,
        ),
    ),
) : NoteEngagementRepository {
    var addCommentCalls: Int = 0
    var nextAddLoveResult: CompletableDeferred<Result<NoteEngagementDto>>? = null

    override suspend fun getEngagement(noteId: String): Result<NoteEngagementDto> = Result.success(initialEngagement)

    override suspend fun addLove(noteId: String): Result<NoteEngagementDto> {
        val deferred = nextAddLoveResult
        return if (deferred != null) {
            deferred.await()
        } else {
            addLoveResult
        }
    }

    override suspend fun removeLove(noteId: String): Result<NoteEngagementDto> = removeLoveResult

    override suspend fun getComments(
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long?,
    ): Result<NoteCommentsPageDto> = commentsPageResult

    override suspend fun addComment(noteId: String, content: String): Result<NoteCommentDto> {
        addCommentCalls += 1
        return addCommentResult
    }
}
