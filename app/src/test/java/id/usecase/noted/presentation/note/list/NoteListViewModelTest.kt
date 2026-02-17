package id.usecase.noted.presentation.note.list

import id.usecase.noted.domain.NoteHistoryRepository
import id.usecase.noted.domain.NoteRepository
import id.usecase.noted.data.sync.LocalSyncStatus
import id.usecase.noted.data.sync.NoteSyncCoordinator
import id.usecase.noted.data.sync.NoteSyncStatus
import id.usecase.noted.data.sync.UserSession
import id.usecase.noted.domain.Note
import id.usecase.noted.domain.NoteHistory
import id.usecase.noted.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initCollectsNotesFromRepository() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)

        repository.emit(
            listOf(
                Note(
                    id = 1,
                    noteId = "n-1",
                    content = "Catatan rapat",
                    createdAt = 1000,
                    updatedAt = 1000,
                    ownerUserId = null,
                    syncStatus = LocalSyncStatus.LOCAL_ONLY,
                ),
                Note(
                    id = 2,
                    noteId = "n-2",
                    content = "Belanja mingguan",
                    createdAt = 2000,
                    updatedAt = 2000,
                    ownerUserId = null,
                    syncStatus = LocalSyncStatus.LOCAL_ONLY,
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.myNotes.size)
        assertEquals("Catatan rapat", viewModel.state.value.myNotes.first().content)
    }

    @Test
    fun addNoteClickedEmitsNavigateToEditorEffect() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteListIntent.AddNoteClicked)
        advanceUntilIdle()

        assertEquals(NoteListEffect.NavigateToEditor(noteId = null), firstEffect.await())
    }

    @Test
    fun searchQueryChangedUpdatesSearchQuery() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)

        viewModel.onIntent(NoteListIntent.SearchQueryChanged("test query"))
        advanceUntilIdle()

        assertEquals("test query", viewModel.state.value.searchQuery)
    }

    @Test
    fun syncClickedEmitsNavigateToSyncEffect() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteListIntent.SyncClicked)
        advanceUntilIdle()

        assertEquals(NoteListEffect.NavigateToSync, firstEffect.await())
    }

    @Test
    fun accountClickedEmitsNavigateToAccountEffect() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteListIntent.AccountClicked)
        advanceUntilIdle()

        assertEquals(NoteListEffect.NavigateToAccount, firstEffect.await())
    }

    @Test
    fun tabSelectedUpdatesSelectedTab() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)

        viewModel.onIntent(NoteListIntent.TabSelected(1))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.selectedTab)
    }

    @Test
    fun historyNoteClickedEmitsNavigateToHistoryNoteEffect() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteListIntent.HistoryNoteClicked("note-123"))
        advanceUntilIdle()

        assertEquals(NoteListEffect.NavigateToHistoryNote(noteId = "note-123"), firstEffect.await())
    }

    @Test
    fun filteredMyNotesFiltersByContent() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)

        repository.emit(
            listOf(
                Note(
                    id = 1,
                    noteId = "n-1",
                    content = "Catatan rapat project",
                    createdAt = 1000,
                    updatedAt = 1000,
                    ownerUserId = null,
                    syncStatus = LocalSyncStatus.LOCAL_ONLY,
                ),
                Note(
                    id = 2,
                    noteId = "n-2",
                    content = "Belanja mingguan",
                    createdAt = 2000,
                    updatedAt = 2000,
                    ownerUserId = null,
                    syncStatus = LocalSyncStatus.LOCAL_ONLY,
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onIntent(NoteListIntent.SearchQueryChanged("rapat"))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.filteredMyNotes.size)
        assertEquals("Catatan rapat project", viewModel.state.value.filteredMyNotes.first().content)
    }

    @Test
    fun filteredMyNotesReturnsAllWhenSearchQueryBlank() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)

        repository.emit(
            listOf(
                Note(
                    id = 1,
                    noteId = "n-1",
                    content = "Catatan rapat",
                    createdAt = 1000,
                    updatedAt = 1000,
                    ownerUserId = null,
                    syncStatus = LocalSyncStatus.LOCAL_ONLY,
                ),
                Note(
                    id = 2,
                    noteId = "n-2",
                    content = "Belanja mingguan",
                    createdAt = 2000,
                    updatedAt = 2000,
                    ownerUserId = null,
                    syncStatus = LocalSyncStatus.LOCAL_ONLY,
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.filteredMyNotes.size)
    }

    @Test
    fun savedNotesAreFilteredFromForkedNotes() = runTest {
        val repository = FakeListRepository()
        val historyRepository = FakeHistoryRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, historyRepository, syncCoordinator)

        repository.emit(
            listOf(
                Note(
                    id = 1,
                    noteId = "n-1",
                    content = "Note saya",
                    createdAt = 1000,
                    updatedAt = 1000,
                    ownerUserId = "user-1",
                    syncStatus = LocalSyncStatus.SYNCED,
                    forkedFrom = null,
                ),
                Note(
                    id = 2,
                    noteId = "n-2",
                    content = "Note tersimpan",
                    createdAt = 2000,
                    updatedAt = 2000,
                    ownerUserId = "user-1",
                    syncStatus = LocalSyncStatus.SYNCED,
                    forkedFrom = "original-123",
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.myNotes.size)
        assertEquals("Note saya", viewModel.state.value.myNotes.first().content)
        assertEquals(1, viewModel.state.value.savedNotes.size)
        assertEquals("Note tersimpan", viewModel.state.value.savedNotes.first().content)
    }
}

private class FakeListRepository(
    var failOnObserve: Boolean = false,
) : NoteRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())

    override fun observeNotes(): Flow<List<Note>> = flow {
        if (failOnObserve) {
            throw IllegalStateException("Load gagal")
        }
        emitAll(notes)
    }

    override suspend fun addNote(content: String): Note {
        val note = Note(
            id = notes.value.size.toLong() + 1,
            noteId = "new-${notes.value.size + 1}",
            content = content,
            createdAt = 10_000,
            updatedAt = 10_000,
            ownerUserId = null,
            syncStatus = LocalSyncStatus.LOCAL_ONLY,
        )
        notes.value = listOf(note) + notes.value
        return note
    }

    override suspend fun getNoteById(noteId: Long): Note? {
        return notes.value.firstOrNull { note -> note.id == noteId }
    }

    override suspend fun updateNote(noteId: Long, content: String): Note? {
        val target = notes.value.firstOrNull { note -> note.id == noteId } ?: return null
        val updated = target.copy(content = content)
        notes.value = notes.value.map { note -> if (note.id == noteId) updated else note }
        return updated
    }

    override suspend fun deleteNote(noteId: Long): Boolean {
        val existingCount = notes.value.size
        notes.value = notes.value.filterNot { note -> note.id == noteId }
        return notes.value.size != existingCount
    }

    fun emit(newNotes: List<Note>) {
        notes.value = newNotes
    }
}

private class FakeHistoryRepository : NoteHistoryRepository {
    private val history = MutableStateFlow<List<NoteHistory>>(emptyList())

    override suspend fun addToHistory(noteId: String, ownerUserId: String, content: String) {
        val newEntry = NoteHistory(
            id = history.value.size.toLong() + 1,
            noteId = noteId,
            ownerUserId = ownerUserId,
            content = content,
            viewedAt = System.currentTimeMillis(),
        )
        history.value = listOf(newEntry) + history.value
    }

    override fun getHistory(limit: Int): Flow<List<NoteHistory>> = flow {
        emitAll(history)
    }

    override suspend fun clearHistory() {
        history.value = emptyList()
    }

    override suspend fun syncHistoryWithServer() {
    }
}

private class FakeSyncCoordinator : NoteSyncCoordinator {
    override val session: StateFlow<UserSession> = MutableStateFlow(UserSession(userId = null, deviceId = "device"))
    override val syncStatus: StateFlow<NoteSyncStatus> = MutableStateFlow(NoteSyncStatus())

    override suspend fun register(username: String, password: String) = Unit

    override suspend fun login(username: String, password: String) = Unit

    override suspend fun forgotPassword(username: String, newPassword: String) = Unit

    override suspend fun signOut() = Unit

    override suspend fun syncNow() = Unit

    override suspend fun uploadPendingNow() = Unit

    override suspend fun importNow() = Unit
}
