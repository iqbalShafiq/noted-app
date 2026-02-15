package id.usecase.backend.note.data

import id.usecase.backend.note.domain.NoteShareRepository
import id.usecase.backend.note.domain.StoredNoteShare
import javax.sql.DataSource

class PostgresNoteShareRepository(
    private val dataSource: DataSource,
) : NoteShareRepository {
    override suspend fun createShare(share: StoredNoteShare): StoredNoteShare {
        dataSource.connection.use { connection ->
            connection.prepareStatement(INSERT_SQL).use { statement ->
                statement.setString(1, share.noteId)
                statement.setString(2, share.recipientUserId)
                statement.setLong(3, share.sharedAtEpochMillis)

                val insertedRows = statement.executeUpdate()
                if (insertedRows == 1) {
                    return share
                }
            }

            connection.prepareStatement(SELECT_BY_COMPOSITE_KEY_SQL).use { statement ->
                statement.setString(1, share.noteId)
                statement.setString(2, share.recipientUserId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.toStoredNoteShare()
                    }
                }
            }
        }

        return share
    }

    override suspend fun findByNoteId(noteId: String): List<StoredNoteShare> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_BY_NOTE_ID_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.executeQuery().use { resultSet ->
                    val shares = mutableListOf<StoredNoteShare>()
                    while (resultSet.next()) {
                        shares += resultSet.toStoredNoteShare()
                    }
                    return shares
                }
            }
        }
    }

    override suspend fun findByRecipientUserId(recipientUserId: String): List<StoredNoteShare> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_BY_RECIPIENT_SQL).use { statement ->
                statement.setString(1, recipientUserId)
                statement.executeQuery().use { resultSet ->
                    val shares = mutableListOf<StoredNoteShare>()
                    while (resultSet.next()) {
                        shares += resultSet.toStoredNoteShare()
                    }
                    return shares
                }
            }
        }
    }
}

private fun java.sql.ResultSet.toStoredNoteShare(): StoredNoteShare {
    return StoredNoteShare(
        noteId = getString("note_id"),
        recipientUserId = getString("recipient_user_id"),
        sharedAtEpochMillis = getLong("shared_at_epoch_millis"),
    )
}

private const val INSERT_SQL = """
    INSERT INTO note_shares (note_id, recipient_user_id, shared_at_epoch_millis)
    VALUES (?, ?, ?)
    ON CONFLICT (note_id, recipient_user_id) DO NOTHING
"""

private const val SELECT_BY_COMPOSITE_KEY_SQL = """
    SELECT note_id, recipient_user_id, shared_at_epoch_millis
    FROM note_shares
    WHERE note_id = ? AND recipient_user_id = ?
    LIMIT 1
"""

private const val SELECT_BY_NOTE_ID_SQL = """
    SELECT note_id, recipient_user_id, shared_at_epoch_millis
    FROM note_shares
    WHERE note_id = ?
    ORDER BY shared_at_epoch_millis DESC
"""

private const val SELECT_BY_RECIPIENT_SQL = """
    SELECT note_id, recipient_user_id, shared_at_epoch_millis
    FROM note_shares
    WHERE recipient_user_id = ?
    ORDER BY shared_at_epoch_millis DESC
"""
