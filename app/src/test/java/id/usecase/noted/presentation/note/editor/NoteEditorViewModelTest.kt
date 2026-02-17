package id.usecase.noted.presentation.note.editor

import androidx.compose.ui.text.input.TextFieldValue
import id.usecase.noted.data.sync.LocalSyncStatus
import id.usecase.noted.domain.Note
import id.usecase.noted.domain.NoteRepository
import id.usecase.noted.domain.NoteVisibility
import id.usecase.noted.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveClickedForNewNotePassesSelectedVisibilityToRepository() = runTest {
        val repository = FakeEditorRepository()
        val viewModel = NoteEditorViewModel(repository)

        val blockId = viewModel.state.value.blocks
            .filterIsInstance<NoteEditorBlock.Text>()
            .first()
            .id

        viewModel.onIntent(
            NoteEditorIntent.TextBlockChanged(
                blockId = blockId,
                value = TextFieldValue("Catatan baru"),
            ),
        )
        viewModel.onIntent(NoteEditorIntent.VisibilityChanged(NoteVisibility.PUBLIC))
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(NoteVisibility.PUBLIC, repository.lastAddedVisibility)
    }

    @Test
    fun saveClickedForExistingNotePassesSelectedVisibilityToRepository() = runTest {
        val repository = FakeEditorRepository(
            seedNotes = listOf(
                createNote(
                    id = 10L,
                    content = "Catatan existing",
                    visibility = NoteVisibility.PRIVATE,
                ),
            ),
        )
        val viewModel = NoteEditorViewModel(repository)

        viewModel.onIntent(NoteEditorIntent.EditorOpened(noteId = 10L))
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.VisibilityChanged(NoteVisibility.LINK_SHARED))
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(NoteVisibility.LINK_SHARED, repository.lastUpdatedVisibility)
    }
}

private class FakeEditorRepository(
    seedNotes: List<Note> = emptyList(),
) : NoteRepository {
    private val notes = MutableStateFlow(seedNotes)

    var lastAddedVisibility: NoteVisibility? = null
        private set

    var lastUpdatedVisibility: NoteVisibility? = null
        private set

    override fun observeNotes(): Flow<List<Note>> = notes.asStateFlow()

    override suspend fun getNoteById(noteId: Long): Note? {
        return notes.value.firstOrNull { note -> note.id == noteId }
    }

    override suspend fun addNote(content: String, visibility: NoteVisibility): Note {
        lastAddedVisibility = visibility
        val note = createNote(
            id = (notes.value.maxOfOrNull(Note::id) ?: 0L) + 1,
            content = content,
            visibility = visibility,
        )
        notes.value = listOf(note) + notes.value
        return note
    }

    override suspend fun updateNote(noteId: Long, content: String, visibility: NoteVisibility): Note? {
        val current = notes.value.firstOrNull { note -> note.id == noteId } ?: return null
        lastUpdatedVisibility = visibility
        val updated = current.copy(content = content, visibility = visibility)
        notes.value = notes.value.map { note -> if (note.id == noteId) updated else note }
        return updated
    }

    override suspend fun deleteNote(noteId: Long): Boolean {
        val before = notes.value.size
        notes.value = notes.value.filterNot { note -> note.id == noteId }
        return before != notes.value.size
    }
}

private fun createNote(
    id: Long,
    content: String,
    visibility: NoteVisibility,
): Note {
    return Note(
        id = id,
        noteId = "note-$id",
        content = content,
        createdAt = 1000L,
        updatedAt = 1000L,
        ownerUserId = null,
        syncStatus = LocalSyncStatus.LOCAL_ONLY,
        visibility = visibility,
    )
}
