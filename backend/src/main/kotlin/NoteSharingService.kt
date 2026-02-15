package id.usecase

import id.usecase.noted.shared.note.CreateNoteRequest
import id.usecase.noted.shared.note.NoteDto
import id.usecase.noted.shared.note.ShareNoteRequest
import id.usecase.noted.shared.note.ShareNoteResponse
import java.util.UUID

class NoteSharingService(
    private val noteRepository: NoteRepository,
    private val noteShareRepository: NoteShareRepository,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun createNote(request: CreateNoteRequest): NoteDto {
        val ownerUserId = request.ownerUserId.trim()
        val content = request.content.trim()
        require(ownerUserId.isNotBlank()) { "ownerUserId must not be blank" }
        require(content.isNotBlank()) { "content must not be blank" }
        val now = clock()

        val stored = noteRepository.create(
            StoredNote(
                id = idGenerator(),
                ownerUserId = ownerUserId,
                content = content,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                deletedAtEpochMillis = null,
                version = 1,
            ),
        )

        return stored.toNoteDto(sharedWithUserIds = emptyList())
    }

    suspend fun getNoteById(noteId: String): NoteDto? {
        val stored = noteRepository.findById(noteId)
            ?.takeIf { it.deletedAtEpochMillis == null }
            ?: return null
        return stored.toNoteDto(sharedWithUserIds = sharedWithUserIds(noteId))
    }

    suspend fun getAccessibleNote(userId: String, noteId: String): NoteDto? {
        val note = getNoteById(noteId) ?: return null
        val canAccess = note.ownerUserId == userId || note.sharedWithUserIds.contains(userId)
        return if (canAccess) note else null
    }

    suspend fun getOwnedNotes(ownerUserId: String): List<NoteDto> {
        val normalizedOwnerUserId = ownerUserId.trim()
        require(normalizedOwnerUserId.isNotBlank()) { "ownerUserId must not be blank" }

        val notes = noteRepository.findByOwner(normalizedOwnerUserId)
            .filter { it.deletedAtEpochMillis == null }
        return notes.map { note ->
            note.toNoteDto(sharedWithUserIds = sharedWithUserIds(note.id))
        }
    }

    suspend fun shareNote(noteId: String, request: ShareNoteRequest): ShareNoteResponse? {
        val recipientUserId = request.recipientUserId.trim()
        require(recipientUserId.isNotBlank()) { "recipientUserId must not be blank" }

        val note = noteRepository.findById(noteId)
            ?.takeIf { it.deletedAtEpochMillis == null }
            ?: return null
        require(recipientUserId != note.ownerUserId) {
            "recipientUserId must be different from ownerUserId"
        }

        val share = noteShareRepository.createShare(
            StoredNoteShare(
                noteId = noteId,
                recipientUserId = recipientUserId,
                sharedAtEpochMillis = clock(),
            ),
        )

        return ShareNoteResponse(
            noteId = share.noteId,
            recipientUserId = share.recipientUserId,
            sharedAtEpochMillis = share.sharedAtEpochMillis,
        )
    }

    suspend fun shareNoteAsOwner(
        ownerUserId: String,
        noteId: String,
        request: ShareNoteRequest,
    ): ShareNoteResponse? {
        val note = noteRepository.findById(noteId)
            ?.takeIf { it.deletedAtEpochMillis == null }
            ?: return null
        if (note.ownerUserId != ownerUserId) {
            throw IllegalArgumentException("Hanya pemilik note yang dapat membagikan")
        }
        return shareNote(noteId = noteId, request = request)
    }

    suspend fun getSharedWith(userId: String): List<NoteDto> {
        val normalizedUserId = userId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }

        val shares = noteShareRepository.findByRecipientUserId(normalizedUserId)
        if (shares.isEmpty()) {
            return emptyList()
        }

        val noteIds = shares.mapTo(linkedSetOf()) { it.noteId }
        val notes = noteRepository.findByIds(noteIds)
        return notes
            .filter { it.deletedAtEpochMillis == null }
            .map { note ->
            note.toNoteDto(sharedWithUserIds = sharedWithUserIds(note.id))
        }
    }

    private suspend fun sharedWithUserIds(noteId: String): List<String> {
        return noteShareRepository.findByNoteId(noteId)
            .asSequence()
            .map { it.recipientUserId }
            .distinct()
            .sorted()
            .toList()
    }
}

private fun StoredNote.toNoteDto(sharedWithUserIds: List<String>): NoteDto {
    return NoteDto(
        id = id,
        ownerUserId = ownerUserId,
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
        sharedWithUserIds = sharedWithUserIds,
    )
}
