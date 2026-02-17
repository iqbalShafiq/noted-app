package id.usecase.backend.data.note

import id.usecase.backend.domain.note.NoteHistoryRepository
import id.usecase.backend.domain.note.StoredNote
import id.usecase.backend.domain.note.StoredNoteHistory
import javax.sql.DataSource

class PostgresNoteHistoryRepository(
    private val dataSource: DataSource,
) : NoteHistoryRepository {
    override suspend fun addHistory(userId: String, noteId: String, note: StoredNote) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(INSERT_OR_UPDATE_SQL).use { statement ->
                statement.setString(1, java.util.UUID.randomUUID().toString())
                statement.setString(2, userId)
                statement.setString(3, noteId)
                statement.setString(4, note.ownerUserId)
                statement.setString(5, note.content)
                statement.setLong(6, System.currentTimeMillis())
                statement.executeUpdate()
            }
        }
    }

    override suspend fun getHistory(userId: String, limit: Int): List<StoredNoteHistory> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_BY_USER_SQL).use { statement ->
                statement.setString(1, userId)
                statement.setInt(2, limit)
                statement.executeQuery().use { resultSet ->
                    val history = mutableListOf<StoredNoteHistory>()
                    while (resultSet.next()) {
                        history += resultSet.toStoredNoteHistory()
                    }
                    return history
                }
            }
        }
    }

    override suspend fun clearHistory(userId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(DELETE_BY_USER_SQL).use { statement ->
                statement.setString(1, userId)
                statement.executeUpdate()
            }
        }
    }
}

private fun java.sql.ResultSet.toStoredNoteHistory(): StoredNoteHistory {
    return StoredNoteHistory(
        id = getString("id"),
        userId = getString("user_id"),
        noteId = getString("note_id"),
        noteOwnerId = getString("note_owner_id"),
        content = getString("content"),
        viewedAtEpochMillis = getLong("viewed_at_epoch_millis"),
    )
}

private const val INSERT_OR_UPDATE_SQL = """
    INSERT INTO note_history (id, user_id, note_id, note_owner_id, content, viewed_at_epoch_millis)
    VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT (user_id, note_id) DO UPDATE SET
        content = EXCLUDED.content,
        viewed_at_epoch_millis = EXCLUDED.viewed_at_epoch_millis
"""

private const val SELECT_BY_USER_SQL = """
    SELECT id, user_id, note_id, note_owner_id, content, viewed_at_epoch_millis
    FROM note_history
    WHERE user_id = ?
    ORDER BY viewed_at_epoch_millis DESC
    LIMIT ?
"""

private const val DELETE_BY_USER_SQL = """
    DELETE FROM note_history
    WHERE user_id = ?
"""
