package id.usecase.noted.presentation.note.editor

import id.usecase.noted.domain.NoteVisibility

data class NoteEditorState(
    val blocks: List<NoteEditorBlock> = listOf(createEmptyTextBlock(1L)),
    val focusedTextBlockId: Long? = 1L,
    val editingNoteId: Long? = null,
    val isLoadingNote: Boolean = false,
    val isSaving: Boolean = false,
    val visibility: NoteVisibility = NoteVisibility.PRIVATE,
    val showShareDialog: Boolean = false,
)
