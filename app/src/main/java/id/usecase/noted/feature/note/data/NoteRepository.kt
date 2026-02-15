package id.usecase.noted.feature.note.data

import id.usecase.noted.feature.note.domain.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>

    suspend fun getNoteById(noteId: Long): Note?

    suspend fun addNote(content: String): Note

    suspend fun updateNote(noteId: Long, content: String): Note?

    suspend fun deleteNote(noteId: Long): Boolean
}
