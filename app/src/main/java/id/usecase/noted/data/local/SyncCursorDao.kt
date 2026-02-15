package id.usecase.noted.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncCursorDao {
    @Query("SELECT cursor FROM sync_cursors WHERE user_id = :userId LIMIT 1")
    suspend fun getCursor(userId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCursor(entity: SyncCursorEntity)

    @Query("DELETE FROM sync_cursors WHERE user_id = :userId")
    suspend fun clearCursorForUser(userId: String)
}
