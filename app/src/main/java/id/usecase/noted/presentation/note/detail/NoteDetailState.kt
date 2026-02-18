package id.usecase.noted.presentation.note.detail

data class NoteDetailState(
    val isLoading: Boolean = false,
    val note: ExternalNote? = null,
    val errorMessage: String? = null,
    val engagement: NoteEngagementUi = NoteEngagementUi(),
    val comments: List<NoteCommentUi> = emptyList(),
    val isCommentsLoading: Boolean = false,
    val isSendingComment: Boolean = false,
    val commentInput: String = "",
    val nextBeforeEpochMillis: Long? = null,
    val hasMoreComments: Boolean = false,
)

data class ExternalNote(
    val id: String,
    val ownerUserId: String,
    val content: String,
    val createdAt: Long,
    val forkedFrom: String?,
)

data class NoteEngagementUi(
    val loveCount: Int = 0,
    val hasLovedByMe: Boolean = false,
    val commentCount: Int = 0,
)

data class NoteCommentUi(
    val id: String,
    val authorUserId: String,
    val authorUsername: String,
    val content: String,
    val createdAtEpochMillis: Long,
)
