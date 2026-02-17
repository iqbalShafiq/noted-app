package id.usecase.noted.domain

import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>

    suspend fun getNoteById(noteId: Long): Note?

    suspend fun addNote(content: String, visibility: NoteVisibility = NoteVisibility.PRIVATE): Note

    suspend fun updateNote(noteId: Long, content: String, visibility: NoteVisibility): Note?

    suspend fun deleteNote(noteId: Long): Boolean
}
