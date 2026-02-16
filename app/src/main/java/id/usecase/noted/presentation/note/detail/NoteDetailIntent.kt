package id.usecase.noted.presentation.note.detail

sealed interface NoteDetailIntent {
    data object ForkClicked : NoteDetailIntent
    data object SaveClicked : NoteDetailIntent
    data object CopyContentClicked : NoteDetailIntent
    data object RetryClicked : NoteDetailIntent
}
