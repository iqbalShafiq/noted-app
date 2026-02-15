package id.usecase.noted.presentation.note.editor

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
}
