package id.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryNoteShareRepository : NoteShareRepository {
    private val shares = mutableListOf<StoredNoteShare>()
    private val mutex = Mutex()

    override suspend fun createShare(share: StoredNoteShare): StoredNoteShare {
        return mutex.withLock {
            val existing = shares.firstOrNull {
                it.noteId == share.noteId && it.recipientUserId == share.recipientUserId
            }
            if (existing != null) {
                return@withLock existing
            }

            shares.add(share)
            share
        }
    }

    override suspend fun findByNoteId(noteId: String): List<StoredNoteShare> {
        return mutex.withLock {
            shares
                .asSequence()
                .filter { it.noteId == noteId }
                .sortedByDescending { it.sharedAtEpochMillis }
                .toList()
        }
    }

    override suspend fun findByRecipientUserId(recipientUserId: String): List<StoredNoteShare> {
        return mutex.withLock {
            shares
                .asSequence()
                .filter { it.recipientUserId == recipientUserId }
                .sortedByDescending { it.sharedAtEpochMillis }
                .toList()
        }
    }
}
