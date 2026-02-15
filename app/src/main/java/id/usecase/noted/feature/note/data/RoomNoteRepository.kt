package id.usecase.noted.feature.note.data

import id.usecase.noted.feature.note.data.local.NoteDao
import id.usecase.noted.feature.note.data.local.NoteEntity
import id.usecase.noted.feature.note.domain.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNoteRepository(
    private val noteDao: NoteDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> {
        return noteDao.observeAllNotes().map { noteEntities ->
            noteEntities.map { entity ->
                entity.toDomain()
            }
        }
    }

    override suspend fun addNote(content: String): Note {
        val createdAt = clock()
        val generatedId = noteDao.insert(
            NoteEntity(
                content = content,
                createdAt = createdAt,
            ),
        )

        return Note(
            id = generatedId,
            content = content,
            createdAt = createdAt,
        )
    }

    override suspend fun getNoteById(noteId: Long): Note? {
        return noteDao.getNoteById(noteId)?.toDomain()
    }

    override suspend fun updateNote(noteId: Long, content: String): Note? {
        val current = noteDao.getNoteById(noteId) ?: return null
        val updatedRows = noteDao.updateContent(noteId = noteId, content = content)
        if (updatedRows == 0) {
            return null
        }

        return current.copy(content = content).toDomain()
    }
}

private fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        content = content,
        createdAt = createdAt,
    )
}
