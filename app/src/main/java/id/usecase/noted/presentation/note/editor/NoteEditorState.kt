package id.usecase.noted.presentation.note.editor

data class NoteEditorState(
    val blocks: List<NoteEditorBlock> = listOf(createEmptyTextBlock(1L)),
    val focusedTextBlockId: Long? = 1L,
    val editingNoteId: Long? = null,
    val isLoadingNote: Boolean = false,
    val isSaving: Boolean = false,
)
