package id.usecase.backend.domain.note

interface NoteCommentRepository {
    suspend fun create(comment: StoredNoteComment): StoredNoteComment

    suspend fun countByNoteId(noteId: String): Int

    suspend fun listByNoteId(
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long? = null,
    ): List<StoredNoteComment>
}
