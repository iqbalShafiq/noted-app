package id.usecase.noted.feature.note.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query("UPDATE notes SET content = :content WHERE id = :noteId")
    suspend fun updateContent(noteId: Long, content: String): Int

    @Insert
    suspend fun insert(note: NoteEntity): Long
}
