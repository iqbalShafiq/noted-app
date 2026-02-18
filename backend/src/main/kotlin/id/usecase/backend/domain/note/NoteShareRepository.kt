package id.usecase.backend.domain.note

data class StoredNoteShare(
    val noteId: String,
    val recipientUserId: String,
    val sharedAtEpochMillis: Long,
)

interface NoteShareRepository {
    suspend fun createShare(share: StoredNoteShare): StoredNoteShare

    suspend fun findByNoteId(noteId: String): List<StoredNoteShare>

    suspend fun findByRecipientUserId(recipientUserId: String): List<StoredNoteShare>

    suspend fun hasShare(noteId: String, recipientUserId: String): Boolean
}
