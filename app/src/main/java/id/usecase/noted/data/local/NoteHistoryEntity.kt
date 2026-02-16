package id.usecase.noted.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_history",
    indices = [
        Index(value = ["note_id"]),
        Index(value = ["viewed_at"]),
    ],
)
data class NoteHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "note_id")
    val noteId: String,
    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: String,
    val content: String,
    @ColumnInfo(name = "viewed_at")
    val viewedAt: Long,
)
