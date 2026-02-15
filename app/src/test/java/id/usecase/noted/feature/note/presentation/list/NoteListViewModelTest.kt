package id.usecase.noted.feature.note.presentation.list

import id.usecase.noted.feature.note.data.NoteRepository
import id.usecase.noted.feature.note.domain.Note
import id.usecase.noted.feature.note.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        val viewModel = NoteListViewModel(repository)

        repository.emit(
            listOf(
                Note(
                    id = 1,
                    content = "Catatan rapat",
                    createdAt = 1000,
                ),
                Note(
                    id = 2,
                    content = "Belanja mingguan",
                    createdAt = 2000,
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.notes.size)
        assertEquals("Catatan rapat", viewModel.state.value.notes.first().content)
    }

    @Test
    fun retryObserveAfterErrorLoadsNotesSuccessfully() = runTest {
        val repository = FakeListRepository(failOnObserve = true)
        val viewModel = NoteListViewModel(repository)

        advanceUntilIdle()
        assertEquals("Load gagal", viewModel.state.value.errorMessage)

        repository.failOnObserve = false
        repository.emit(
            listOf(
                Note(
                    id = 9,
                    content = "Hasil retry berhasil",
                    createdAt = 3000,
                ),
            ),
        )
        viewModel.onIntent(NoteListIntent.RetryObserve)
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.errorMessage)
        assertEquals("Hasil retry berhasil", viewModel.state.value.notes.first().content)
    }

    @Test
    fun addNoteClickedEmitsNavigateToEditorEffect() = runTest {
        val repository = FakeListRepository()
        val viewModel = NoteListViewModel(repository)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteListIntent.AddNoteClicked)
        advanceUntilIdle()

        assertEquals(NoteListEffect.NavigateToEditor(noteId = null), firstEffect.await())
    }

    @Test
    fun noteClickedEmitsNavigateToEditorWithSelectedId() = runTest {
        val repository = FakeListRepository()
        val viewModel = NoteListViewModel(repository)
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteListIntent.NoteClicked(noteId = 42L))
        advanceUntilIdle()

        assertEquals(NoteListEffect.NavigateToEditor(noteId = 42L), firstEffect.await())
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
            content = content,
            createdAt = 10_000,
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

    fun emit(newNotes: List<Note>) {
        notes.value = newNotes
    }
}
