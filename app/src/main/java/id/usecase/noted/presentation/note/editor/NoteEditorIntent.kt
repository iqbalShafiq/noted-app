package id.usecase.noted.presentation.note.editor

import androidx.compose.ui.text.input.TextFieldValue

sealed interface NoteEditorIntent {
    data class EditorOpened(val noteId: Long?) : NoteEditorIntent

    data class TextBlockChanged(
        val blockId: Long,
        val value: TextFieldValue,
    ) : NoteEditorIntent

    data class TextBlockFocused(val blockId: Long) : NoteEditorIntent

    data object InsertPhotoFromCameraClicked : NoteEditorIntent

    data object InsertPhotoFromGalleryClicked : NoteEditorIntent

    data object TagLocationClicked : NoteEditorIntent

    data class PhotoPicked(val uri: String) : NoteEditorIntent

    data class RemoveImageClicked(val blockId: Long) : NoteEditorIntent

    data object RemoveLocationClicked : NoteEditorIntent

    data class LocationTagged(
        val latitude: Double,
        val longitude: Double,
        val label: String,
    ) : NoteEditorIntent

    data object SaveClicked : NoteEditorIntent

    data object DeleteClicked : NoteEditorIntent
}
