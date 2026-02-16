package id.usecase.noted.presentation.note.editor

import android.graphics.Bitmap
import id.usecase.noted.domain.NoteVisibility

data class NoteEditorLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

sealed interface NoteEditorEffect {
    data object NavigateToCamera : NoteEditorEffect

    data class NavigateToLocationPicker(
        val initialLocation: NoteEditorLocation?,
    ) : NoteEditorEffect

    data object LaunchPhotoPicker : NoteEditorEffect

    data class ShowMessage(val message: String) : NoteEditorEffect

    data object NavigateToList : NoteEditorEffect

    data class ShareNote(val noteId: String, val visibility: NoteVisibility) : NoteEditorEffect

    data class CopyLink(val link: String) : NoteEditorEffect

    data class ShowQRCode(val bitmap: Bitmap) : NoteEditorEffect
}
