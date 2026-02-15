package id.usecase.noted.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["note_id"], unique = true),
        Index(value = ["owner_user_id"]),
        Index(value = ["sync_status"]),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "note_id")
    val noteId: String,
    val content: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: String? = null,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String,
    @ColumnInfo(name = "server_version")
    val serverVersion: Long? = null,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_error_message")
    val syncErrorMessage: String? = null,
)
