package id.usecase.noted.feature.note.presentation.list

import id.usecase.noted.feature.note.data.NoteRepository
import id.usecase.noted.feature.note.data.sync.NoteSyncCoordinator
import id.usecase.noted.feature.note.data.sync.NoteSyncStatus
import id.usecase.noted.feature.note.data.sync.UserSession
import id.usecase.noted.feature.note.domain.Note
import id.usecase.noted.feature.note.data.sync.LocalSyncStatus
import id.usecase.noted.feature.note.presentation.MainDispatcherRule
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
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, syncCoordinator)

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

        assertEquals(2, viewModel.state.value.notes.size)
        assertEquals("Catatan rapat", viewModel.state.value.notes.first().content)
    }

    @Test
    fun addNoteClickedEmitsNavigateToEditorEffect() = runTest {
        val repository = FakeListRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, syncCoordinator)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteListIntent.AddNoteClicked)
        advanceUntilIdle()

        assertEquals(NoteListEffect.NavigateToEditor(noteId = null), firstEffect.await())
    }

    @Test
    fun loginSubmitCallsLogin() = runTest {
        val repository = FakeListRepository()
        val syncCoordinator = FakeSyncCoordinator()
        val viewModel = NoteListViewModel(repository, syncCoordinator)

        viewModel.onIntent(NoteListIntent.LoginInputChanged("qa-user"))
        viewModel.onIntent(NoteListIntent.PasswordInputChanged("password123"))
        viewModel.onIntent(NoteListIntent.LoginSubmitClicked)
        advanceUntilIdle()

        assertEquals("qa-user", syncCoordinator.lastLoginUsername)
        assertEquals("password123", syncCoordinator.lastLoginPassword)
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

private class FakeSyncCoordinator : NoteSyncCoordinator {
    override val session: StateFlow<UserSession> = MutableStateFlow(UserSession(userId = null, deviceId = "device"))
    override val syncStatus: StateFlow<NoteSyncStatus> = MutableStateFlow(NoteSyncStatus())

    var lastRegisterUsername: String? = null
    var lastRegisterPassword: String? = null
    var lastLoginUsername: String? = null
    var lastLoginPassword: String? = null

    override suspend fun register(username: String, password: String) {
        lastRegisterUsername = username
        lastRegisterPassword = password
    }

    override suspend fun login(username: String, password: String) {
        lastLoginUsername = username
        lastLoginPassword = password
    }

    override suspend fun signOut() = Unit

    override suspend fun syncNow() = Unit

    override suspend fun uploadPendingNow() = Unit

    override suspend fun importNow() = Unit
}
