package id.usecase.noted.feature.note.presentation.editor

import id.usecase.noted.feature.note.data.NoteRepository
import id.usecase.noted.feature.note.data.sync.LocalSyncStatus
import id.usecase.noted.feature.note.domain.NoteContentBlock
import id.usecase.noted.feature.note.domain.NoteContentCodec
import id.usecase.noted.feature.note.domain.Note
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import id.usecase.noted.feature.note.presentation.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun textBlockChangedUpdatesState() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val blockId = (viewModel.state.value.blocks.first() as NoteEditorBlock.Text).id

        viewModel.onIntent(
            NoteEditorIntent.TextBlockChanged(
                blockId = blockId,
                value = TextFieldValue("Catatan baru"),
            ),
        )

        assertEquals(
            "Catatan baru",
            (viewModel.state.value.blocks.first() as NoteEditorBlock.Text).value.text,
        )
    }

    @Test
    fun saveClickedWithEmptyBlocksEmitsValidationEffect() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(NoteEditorEffect.ShowMessage("Isi note tidak boleh kosong"), firstEffect.await())
    }

    @Test
    fun galleryButtonClickEmitsLaunchPhotoPickerEffect() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteEditorIntent.InsertPhotoFromGalleryClicked)
        advanceUntilIdle()

        assertEquals(NoteEditorEffect.LaunchPhotoPicker, firstEffect.await())
    }

    @Test
    fun cameraButtonClickEmitsNavigateToCameraEffect() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteEditorIntent.InsertPhotoFromCameraClicked)
        advanceUntilIdle()

        assertEquals(NoteEditorEffect.NavigateToCamera, firstEffect.await())
    }

    @Test
    fun locationButtonClickEmitsNavigateToLocationPickerEffect() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(NoteEditorIntent.TagLocationClicked)
        advanceUntilIdle()

        assertEquals(
            NoteEditorEffect.NavigateToLocationPicker(initialLocation = null),
            firstEffect.await(),
        )
    }

    @Test
    fun locationButtonClickWithExistingLocationEmitsNavigateToLocationPickerWithSeed() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val effects = mutableListOf<NoteEditorEffect>()
        val effectJob = launch {
            viewModel.effect.take(2).toList(effects)
        }

        viewModel.onIntent(
            NoteEditorIntent.LocationTagged(
                latitude = -6.175392,
                longitude = 106.827153,
                label = "Monas",
            ),
        )
        viewModel.onIntent(NoteEditorIntent.TagLocationClicked)
        advanceUntilIdle()

        assertEquals(
            NoteEditorEffect.NavigateToLocationPicker(
                initialLocation = NoteEditorLocation(
                    latitude = -6.175392,
                    longitude = 106.827153,
                    label = "Monas",
                ),
            ),
            effects.last(),
        )

        effectJob.cancel()
    }

    @Test
    fun editorOpenedWithExistingNoteLoadsBlocksAndEditingId() = runTest {
        val repository = FakeNoteRepository().apply {
            seedNote(
                Note(
                    id = 7L,
                    noteId = "note-7",
                    content = NoteContentCodec.encode(
                        listOf(
                            NoteContentBlock.Text("Catatan lama"),
                            NoteContentBlock.Image("content://gallery/old-photo"),
                        ),
                    ),
                    createdAt = 123L,
                    updatedAt = 123L,
                    ownerUserId = null,
                    syncStatus = LocalSyncStatus.LOCAL_ONLY,
                ),
            )
        }
        val viewModel = NoteEditorViewModel(repository)

        viewModel.onIntent(NoteEditorIntent.EditorOpened(noteId = 7L))
        advanceUntilIdle()

        assertEquals(7L, viewModel.state.value.editingNoteId)
        val blocks = viewModel.state.value.blocks
        assertEquals(2, blocks.size)
        assertEquals("Catatan lama", (blocks[0] as NoteEditorBlock.Text).value.text)
        assertEquals("content://gallery/old-photo", (blocks[1] as NoteEditorBlock.Image).uri)
    }

    @Test
    fun saveClickedWhileEditingUpdatesNoteInsteadOfAddingNewOne() = runTest {
        val repository = FakeNoteRepository().apply {
            seedNote(
                Note(
                    id = 19L,
                    noteId = "note-19",
                    content = NoteContentCodec.encode(listOf(NoteContentBlock.Text("Draft"))),
                    createdAt = 222L,
                    updatedAt = 222L,
                    ownerUserId = null,
                    syncStatus = LocalSyncStatus.LOCAL_ONLY,
                ),
            )
        }
        val viewModel = NoteEditorViewModel(repository)
        val effects = mutableListOf<NoteEditorEffect>()
        val effectJob = launch {
            viewModel.effect.take(2).toList(effects)
        }

        viewModel.onIntent(NoteEditorIntent.EditorOpened(noteId = 19L))
        advanceUntilIdle()
        val firstText = viewModel.state.value.blocks.first() as NoteEditorBlock.Text
        viewModel.onIntent(
            NoteEditorIntent.TextBlockChanged(
                blockId = firstText.id,
                value = TextFieldValue("Draft yang diperbarui"),
            ),
        )
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(0, repository.insertedContents.size)
        val updatedContent = repository.updatedContents.getValue(19L)
        assertEquals(
            listOf(NoteContentBlock.Text("Draft yang diperbarui")),
            NoteContentCodec.decode(updatedContent),
        )
        assertEquals(
            listOf(
                NoteEditorEffect.ShowMessage("Note berhasil diperbarui"),
                NoteEditorEffect.NavigateToList,
            ),
            effects,
        )

        effectJob.cancel()
    }

    @Test
    fun photoPickedInMiddleOfTextCreatesImageBlockBetweenTextBlocks() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val firstText = viewModel.state.value.blocks.first() as NoteEditorBlock.Text

        viewModel.onIntent(
            NoteEditorIntent.TextBlockChanged(
                blockId = firstText.id,
                value = TextFieldValue(
                    text = "Halo dunia",
                    selection = TextRange(5),
                ),
            ),
        )
        viewModel.onIntent(NoteEditorIntent.TextBlockFocused(firstText.id))

        viewModel.onIntent(NoteEditorIntent.PhotoPicked("content://gallery/photo-1"))
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.blocks.size)
        val blocks = viewModel.state.value.blocks
        assertEquals("Halo ", (blocks[0] as NoteEditorBlock.Text).value.text)
        assertEquals("content://gallery/photo-1", (blocks[1] as NoteEditorBlock.Image).uri)
        assertEquals("dunia", (blocks[2] as NoteEditorBlock.Text).value.text)
    }

    @Test
    fun locationTaggedInMiddleOfTextCreatesLocationBlockBetweenTextBlocks() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val firstText = viewModel.state.value.blocks.first() as NoteEditorBlock.Text

        viewModel.onIntent(
            NoteEditorIntent.TextBlockChanged(
                blockId = firstText.id,
                value = TextFieldValue(
                    text = "Rapat di kantor pusat",
                    selection = TextRange(8),
                ),
            ),
        )
        viewModel.onIntent(NoteEditorIntent.TextBlockFocused(firstText.id))

        viewModel.onIntent(
            NoteEditorIntent.LocationTagged(
                latitude = -6.175392,
                longitude = 106.827153,
                label = "Monas",
            ),
        )
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.blocks.size)
        val blocks = viewModel.state.value.blocks
        assertEquals("Rapat di", (blocks[0] as NoteEditorBlock.Text).value.text)
        assertEquals("Monas", (blocks[1] as NoteEditorBlock.Location).label)
        assertEquals(" kantor pusat", (blocks[2] as NoteEditorBlock.Text).value.text)
    }

    @Test
    fun saveClickedWithTextAndImagePersistsEncodedContentAndNavigates() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = NoteEditorViewModel(repository)
        val effects = mutableListOf<NoteEditorEffect>()
        val effectJob = launch {
            viewModel.effect.take(2).toList(effects)
        }
        val firstText = viewModel.state.value.blocks.first() as NoteEditorBlock.Text

        viewModel.onIntent(
            NoteEditorIntent.TextBlockChanged(
                blockId = firstText.id,
                value = TextFieldValue(
                    text = "Belajar Navigation 3",
                    selection = TextRange("Belajar Navigation 3".length),
                ),
            ),
        )
        viewModel.onIntent(NoteEditorIntent.TextBlockFocused(firstText.id))
        viewModel.onIntent(NoteEditorIntent.PhotoPicked("content://gallery/photo-1"))
        val trailingTextId = (viewModel.state.value.blocks.last() as NoteEditorBlock.Text).id
        viewModel.onIntent(NoteEditorIntent.TextBlockFocused(trailingTextId))
        viewModel.onIntent(
            NoteEditorIntent.LocationTagged(
                latitude = -6.175392,
                longitude = 106.827153,
                label = "Monas",
            ),
        )
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        val decodedBlocks = NoteContentCodec.decode(repository.insertedContents.single())
        assertEquals(
            listOf(
                NoteContentBlock.Text("Belajar Navigation 3"),
                NoteContentBlock.Image("content://gallery/photo-1"),
                NoteContentBlock.Location(
                    latitude = -6.175392,
                    longitude = 106.827153,
                    label = "Monas",
                ),
                NoteContentBlock.Text(""),
            ),
            decodedBlocks,
        )

        assertFalse(viewModel.state.value.isSaving)
        assertEquals(1, viewModel.state.value.blocks.size)
        assertEquals("", (viewModel.state.value.blocks.first() as NoteEditorBlock.Text).value.text)
        assertEquals(
            listOf(
                NoteEditorEffect.ShowMessage("Note berhasil disimpan"),
                NoteEditorEffect.NavigateToList,
            ),
            effects,
        )

        effectJob.cancel()
    }

    @Test
    fun secondSaveClickWhileSavingInsertsOnlyOnce() = runTest {
        val repository = FakeNoteRepository(insertDelayMillis = 1_000)
        val viewModel = NoteEditorViewModel(repository)
        val firstText = viewModel.state.value.blocks.first() as NoteEditorBlock.Text

        viewModel.onIntent(
            NoteEditorIntent.TextBlockChanged(
                blockId = firstText.id,
                value = TextFieldValue("Note penting"),
            ),
        )
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        runCurrent()
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, repository.insertedContents.size)
    }

    @Test
    fun removeImageClickedDeletesSelectedImageBlock() = runTest {
        val viewModel = NoteEditorViewModel(FakeNoteRepository())
        val firstText = viewModel.state.value.blocks.first() as NoteEditorBlock.Text

        viewModel.onIntent(
            NoteEditorIntent.TextBlockChanged(
                blockId = firstText.id,
                value = TextFieldValue("Sisip foto lalu hapus"),
            ),
        )
        viewModel.onIntent(NoteEditorIntent.TextBlockFocused(firstText.id))
        viewModel.onIntent(NoteEditorIntent.PhotoPicked("content://gallery/photo-to-remove"))
        advanceUntilIdle()

        val imageBlockId = viewModel.state.value.blocks
            .filterIsInstance<NoteEditorBlock.Image>()
            .first()
            .id

        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(blockId = imageBlockId))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.blocks.none { block -> block is NoteEditorBlock.Image })
    }
}

private class FakeNoteRepository(
    private val insertDelayMillis: Long = 0,
) : NoteRepository {
    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    val insertedContents = mutableListOf<String>()
    val updatedContents = mutableMapOf<Long, String>()

    override fun observeNotes(): Flow<List<Note>> = notesFlow

    override suspend fun addNote(content: String): Note {
        if (insertDelayMillis > 0) {
            delay(insertDelayMillis)
        }

        insertedContents += content
        val inserted = Note(
            id = notesFlow.value.size.toLong() + 1,
            noteId = "note-${notesFlow.value.size + 1}",
            content = content,
            createdAt = 10_000,
            updatedAt = 10_000,
            ownerUserId = null,
            syncStatus = LocalSyncStatus.LOCAL_ONLY,
        )
        notesFlow.value = listOf(inserted) + notesFlow.value
        return inserted
    }

    override suspend fun getNoteById(noteId: Long): Note? {
        return notesFlow.value.firstOrNull { note -> note.id == noteId }
    }

    override suspend fun updateNote(noteId: Long, content: String): Note? {
        val target = notesFlow.value.firstOrNull { note -> note.id == noteId } ?: return null
        updatedContents[noteId] = content
        val updated = target.copy(content = content)
        notesFlow.value = notesFlow.value.map { note ->
            if (note.id == noteId) updated else note
        }
        return updated
    }

    override suspend fun deleteNote(noteId: Long): Boolean {
        val exists = notesFlow.value.any { it.id == noteId }
        if (!exists) {
            return false
        }
        notesFlow.value = notesFlow.value.filterNot { it.id == noteId }
        return true
    }

    fun seedNote(note: Note) {
        notesFlow.value = listOf(note) + notesFlow.value.filterNot { current -> current.id == note.id }
    }
}
