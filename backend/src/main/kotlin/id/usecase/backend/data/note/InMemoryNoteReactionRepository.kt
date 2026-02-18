package id.usecase.backend.data.note

import id.usecase.backend.domain.note.NoteLoveSnapshot
import id.usecase.backend.domain.note.NoteReactionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryNoteReactionRepository : NoteReactionRepository {
    private val loves = mutableSetOf<Pair<String, String>>()
    private val mutex = Mutex()

    override suspend fun addLove(noteId: String, userId: String): Boolean {
        return mutex.withLock {
            loves.add(noteId to userId)
        }
    }

    override suspend fun removeLove(noteId: String, userId: String): Boolean {
        return mutex.withLock {
            loves.remove(noteId to userId)
        }
    }

    override suspend fun hasLoved(noteId: String, userId: String): Boolean {
        return mutex.withLock {
            (noteId to userId) in loves
        }
    }

    override suspend fun countLoves(noteId: String): Int {
        return mutex.withLock {
            loves.count { (storedNoteId, _) -> storedNoteId == noteId }
        }
    }

    override suspend fun getLoveSnapshot(noteId: String, userId: String): NoteLoveSnapshot {
        return mutex.withLock {
            NoteLoveSnapshot(
                loveCount = loves.count { (storedNoteId, _) -> storedNoteId == noteId },
                hasLovedByMe = (noteId to userId) in loves,
            )
        }
    }
}
