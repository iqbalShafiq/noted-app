package id.usecase.noted.presentation.note.detail

sealed interface NoteDetailIntent {
    data class NoteOpened(val noteId: String) : NoteDetailIntent
    data object ForkClicked : NoteDetailIntent
    data object SaveClicked : NoteDetailIntent
    data object CopyContentClicked : NoteDetailIntent
    data object RetryClicked : NoteDetailIntent
    data object LoveClicked : NoteDetailIntent
    data object CommentsClicked : NoteDetailIntent
    data class CommentInputChanged(val value: String) : NoteDetailIntent
    data object SubmitCommentClicked : NoteDetailIntent
    data object LoadMoreComments : NoteDetailIntent
    data object RefreshEngagement : NoteDetailIntent
}
