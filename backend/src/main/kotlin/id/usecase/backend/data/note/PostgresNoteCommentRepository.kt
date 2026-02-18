package id.usecase.backend.data.note

import id.usecase.backend.domain.note.NoteCommentRepository
import id.usecase.backend.domain.note.StoredNoteComment
import javax.sql.DataSource

class PostgresNoteCommentRepository(
    private val dataSource: DataSource,
) : NoteCommentRepository {
    override suspend fun create(comment: StoredNoteComment): StoredNoteComment {
        dataSource.connection.use { connection ->
            connection.prepareStatement(INSERT_SQL).use { statement ->
                statement.setString(1, comment.id)
                statement.setString(2, comment.noteId)
                statement.setString(3, comment.authorUserId)
                statement.setString(4, comment.content)
                statement.setLong(5, comment.createdAtEpochMillis)
                statement.executeUpdate()
            }
        }
        return comment
    }

    override suspend fun countByNoteId(noteId: String): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement(COUNT_BY_NOTE_ID_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) {
                        resultSet.getInt("comment_count")
                    } else {
                        0
                    }
                }
            }
        }
    }

    override suspend fun listByNoteId(
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long?,
    ): List<StoredNoteComment> {
        require(limit > 0) { "limit must be positive" }

        val limited = queryComments(
            noteId = noteId,
            beforeEpochMillis = beforeEpochMillis,
            limit = limit,
        )

        if (limited.size < limit) {
            return limited
        }

        val boundary = limited.last()
        val expanded = queryBoundaryTail(
            noteId = noteId,
            createdAtEpochMillis = boundary.createdAtEpochMillis,
            lastSeenId = boundary.id,
        )
        return limited + expanded
    }

    private fun queryComments(
        noteId: String,
        beforeEpochMillis: Long?,
        limit: Int,
    ): List<StoredNoteComment> {
        dataSource.connection.use { connection ->
            val sql = if (beforeEpochMillis == null) {
                LIST_BY_NOTE_ID_SQL
            } else {
                LIST_BY_NOTE_ID_BEFORE_SQL
            }

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, noteId)
                if (beforeEpochMillis == null) {
                    statement.setInt(2, limit)
                } else {
                    statement.setLong(2, beforeEpochMillis)
                    statement.setInt(3, limit)
                }

                statement.executeQuery().use { resultSet ->
                    val rows = mutableListOf<StoredNoteComment>()
                    while (resultSet.next()) {
                        rows += resultSet.toStoredNoteComment()
                    }
                    return rows
                }
            }
        }
    }

    private fun queryBoundaryTail(
        noteId: String,
        createdAtEpochMillis: Long,
        lastSeenId: String,
    ): List<StoredNoteComment> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(LIST_BOUNDARY_TAIL_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.setLong(2, createdAtEpochMillis)
                statement.setString(3, lastSeenId)
                statement.executeQuery().use { resultSet ->
                    val rows = mutableListOf<StoredNoteComment>()
                    while (resultSet.next()) {
                        rows += resultSet.toStoredNoteComment()
                    }
                    return rows
                }
            }
        }
    }
}

private fun java.sql.ResultSet.toStoredNoteComment(): StoredNoteComment {
    return StoredNoteComment(
        id = getString("id"),
        noteId = getString("note_id"),
        authorUserId = getString("author_user_id"),
        content = getString("content"),
        createdAtEpochMillis = getLong("created_at_epoch_millis"),
    )
}

private const val INSERT_SQL = """
    INSERT INTO note_comments (id, note_id, author_user_id, content, created_at_epoch_millis)
    VALUES (?, ?, ?, ?, ?)
"""

private const val COUNT_BY_NOTE_ID_SQL = """
    SELECT COUNT(*)::INT AS comment_count
    FROM note_comments
    WHERE note_id = ?
"""

private const val LIST_BY_NOTE_ID_SQL = """
    SELECT id, note_id, author_user_id, content, created_at_epoch_millis
    FROM note_comments
    WHERE note_id = ?
    ORDER BY created_at_epoch_millis DESC, id DESC
    LIMIT ?
"""

private const val LIST_BY_NOTE_ID_BEFORE_SQL = """
    SELECT id, note_id, author_user_id, content, created_at_epoch_millis
    FROM note_comments
    WHERE note_id = ? AND created_at_epoch_millis < ?
    ORDER BY created_at_epoch_millis DESC, id DESC
    LIMIT ?
"""

private const val LIST_BOUNDARY_TAIL_SQL = """
    SELECT id, note_id, author_user_id, content, created_at_epoch_millis
    FROM note_comments
    WHERE note_id = ? AND created_at_epoch_millis = ? AND id < ?
    ORDER BY created_at_epoch_millis DESC, id DESC
"""
