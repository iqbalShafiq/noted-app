package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.NoteCommentDto
import id.usecase.noted.shared.note.NoteCommentsPageDto
import id.usecase.noted.shared.note.NoteEngagementDto
import kotlinx.coroutines.CancellationException

interface NoteEngagementRepository {
    suspend fun getEngagement(noteId: String): Result<NoteEngagementDto>

    suspend fun addLove(noteId: String): Result<NoteEngagementDto>

    suspend fun removeLove(noteId: String): Result<NoteEngagementDto>

    suspend fun getComments(
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long? = null,
    ): Result<NoteCommentsPageDto>

    suspend fun addComment(noteId: String, content: String): Result<NoteCommentDto>
}

class SyncNoteEngagementRepository(
    private val noteEngagementApi: NoteEngagementApi,
    private val sessionStore: SessionStore,
) : NoteEngagementRepository {
    override suspend fun getEngagement(noteId: String): Result<NoteEngagementDto> {
        return withAccessToken { accessToken ->
            noteEngagementApi.getEngagement(accessToken = accessToken, noteId = noteId)
        }
    }

    override suspend fun addLove(noteId: String): Result<NoteEngagementDto> {
        return withAccessToken { accessToken ->
            noteEngagementApi.addLove(accessToken = accessToken, noteId = noteId)
        }
    }

    override suspend fun removeLove(noteId: String): Result<NoteEngagementDto> {
        return withAccessToken { accessToken ->
            noteEngagementApi.removeLove(accessToken = accessToken, noteId = noteId)
        }
    }

    override suspend fun getComments(
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long?,
    ): Result<NoteCommentsPageDto> {
        return withAccessToken { accessToken ->
            noteEngagementApi.getComments(
                accessToken = accessToken,
                noteId = noteId,
                limit = limit,
                beforeEpochMillis = beforeEpochMillis,
            )
        }
    }

    override suspend fun addComment(noteId: String, content: String): Result<NoteCommentDto> {
        return withAccessToken { accessToken ->
            noteEngagementApi.addComment(
                accessToken = accessToken,
                noteId = noteId,
                content = content,
            )
        }
    }

    private suspend inline fun <T> withAccessToken(crossinline block: suspend (String) -> T): Result<T> {
        val accessToken = sessionStore.currentSession().accessToken
            ?: return Result.failure(IllegalStateException("Login diperlukan"))

        return try {
            Result.success(block(accessToken))
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            Result.failure(error)
        }
    }
}
