package id.usecase

data class StoredNoteShare(
    val noteId: String,
    val recipientUserId: String,
    val sharedAtEpochMillis: Long,
)

interface NoteShareRepository {
    suspend fun createShare(share: StoredNoteShare): StoredNoteShare

    suspend fun findByNoteId(noteId: String): List<StoredNoteShare>

    suspend fun findByRecipientUserId(recipientUserId: String): List<StoredNoteShare>
}
