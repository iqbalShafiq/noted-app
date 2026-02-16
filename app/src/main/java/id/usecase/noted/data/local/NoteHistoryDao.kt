package id.usecase.noted.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteHistoryDao {
    @Insert
    suspend fun insert(history: NoteHistoryEntity): Long

    @Query("SELECT * FROM note_history ORDER BY viewed_at DESC LIMIT :limit")
    fun getHistory(limit: Int = 50): Flow<List<NoteHistoryEntity>>

    @Query("DELETE FROM note_history")
    suspend fun clearHistory()

    @Query("SELECT EXISTS(SELECT 1 FROM note_history WHERE note_id = :noteId)")
    suspend fun hasHistory(noteId: String): Boolean
}
