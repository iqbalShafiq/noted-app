package id.usecase.noted.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deleted_at IS NULL ORDER BY created_at DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId AND deleted_at IS NULL LIMIT 1")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE note_id = :remoteNoteId LIMIT 1")
    suspend fun getByRemoteId(remoteNoteId: String): NoteEntity?

    @Query(
        """
        SELECT * FROM notes
        WHERE owner_user_id = :ownerUserId
          AND sync_status IN ('PENDING_UPSERT', 'PENDING_DELETE', 'SYNC_ERROR')
        ORDER BY updated_at ASC
        """,
    )
    suspend fun getPendingMutations(ownerUserId: String): List<NoteEntity>

    @Query(
        """
        SELECT COUNT(*) FROM notes
        WHERE owner_user_id = :ownerUserId
          AND sync_status IN ('PENDING_UPSERT', 'PENDING_DELETE', 'SYNC_ERROR')
        """,
    )
    suspend fun countPendingMutations(ownerUserId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM notes
        WHERE owner_user_id IS NULL
          AND sync_status = 'LOCAL_ONLY'
          AND deleted_at IS NULL
        """,
    )
    suspend fun countAnonymousLocalOnly(): Int

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query(
        """
        UPDATE notes
        SET content = :content,
            updated_at = :updatedAt,
            owner_user_id = :ownerUserId,
            sync_status = :syncStatus,
            sync_error_message = NULL
        WHERE id = :noteId
        """,
    )
    suspend fun updateContent(
        noteId: Long,
        content: String,
        updatedAt: Long,
        ownerUserId: String?,
        syncStatus: String,
    ): Int

    @Query(
        """
        UPDATE notes
        SET updated_at = :updatedAt,
            deleted_at = :deletedAt,
            owner_user_id = :ownerUserId,
            sync_status = :syncStatus,
            sync_error_message = NULL
        WHERE id = :noteId
        """,
    )
    suspend fun markPendingDelete(
        noteId: Long,
        updatedAt: Long,
        deletedAt: Long,
        ownerUserId: String?,
        syncStatus: String,
    ): Int

    @Query(
        """
        UPDATE notes
        SET owner_user_id = :ownerUserId,
            sync_status = 'PENDING_UPSERT'
        WHERE owner_user_id IS NULL
          AND deleted_at IS NULL
          AND sync_status IN ('LOCAL_ONLY', 'SYNC_ERROR')
        """,
    )
    suspend fun assignAnonymousNotesToOwner(ownerUserId: String)

    @Query(
        """
        UPDATE notes
        SET content = :content,
            updated_at = :updatedAt,
            deleted_at = :deletedAt,
            owner_user_id = :ownerUserId,
            sync_status = 'SYNCED',
            server_version = :serverVersion,
            sync_error_message = NULL
        WHERE id = :localId
        """,
    )
    suspend fun markSyncedByLocalId(
        localId: Long,
        ownerUserId: String,
        content: String,
        updatedAt: Long,
        deletedAt: Long?,
        serverVersion: Long,
    )

    @Query(
        """
        UPDATE notes
        SET sync_status = 'SYNC_ERROR',
            sync_error_message = :errorMessage
        WHERE note_id = :remoteNoteId
        """,
    )
    suspend fun markSyncError(remoteNoteId: String, errorMessage: String)

    @Query(
        """
        UPDATE notes
        SET sync_status = 'SYNC_ERROR',
            sync_error_message = :errorMessage
        WHERE id = :localId
        """,
    )
    suspend fun markSyncErrorByLocalId(localId: Long, errorMessage: String)

    @Query(
        """
        UPDATE notes
        SET content = :content,
            updated_at = :updatedAt,
            deleted_at = :deletedAt,
            owner_user_id = :ownerUserId,
            sync_status = :syncStatus,
            server_version = :serverVersion,
            sync_error_message = :syncErrorMessage
        WHERE id = :localId
        """,
    )
    suspend fun updateFromServerByLocalId(
        localId: Long,
        ownerUserId: String,
        content: String,
        updatedAt: Long,
        deletedAt: Long?,
        syncStatus: String,
        serverVersion: Long,
        syncErrorMessage: String?,
    )

    @Query("DELETE FROM notes WHERE id = :localId")
    suspend fun deleteByLocalId(localId: Long)

    @Query("DELETE FROM notes WHERE note_id = :remoteNoteId")
    suspend fun deleteByRemoteId(remoteNoteId: String)

    @Query(
        """
        SELECT * FROM notes 
        WHERE visibility = :visibility 
          AND deleted_at IS NULL 
        ORDER BY updated_at DESC
        """,
    )
    fun getNotesByVisibility(visibility: String): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes 
        WHERE visibility != 'PRIVATE' 
          AND owner_user_id != :ownerId 
          AND deleted_at IS NULL 
        ORDER BY updated_at DESC
        """,
    )
    fun getSharedNotesExcludingOwner(ownerId: String): Flow<List<NoteEntity>>
}
