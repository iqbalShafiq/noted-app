package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.NoteCommentDto
import id.usecase.noted.shared.note.NoteCommentsPageDto
import id.usecase.noted.shared.note.NoteEngagementDto

interface NoteEngagementApi {
    suspend fun getEngagement(accessToken: String, noteId: String): NoteEngagementDto

    suspend fun addLove(accessToken: String, noteId: String): NoteEngagementDto

    suspend fun removeLove(accessToken: String, noteId: String): NoteEngagementDto

    suspend fun getComments(
        accessToken: String,
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long?,
    ): NoteCommentsPageDto

    suspend fun addComment(accessToken: String, noteId: String, content: String): NoteCommentDto
}
