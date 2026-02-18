package id.usecase.backend.data.note

import id.usecase.backend.domain.note.NoteLoveSnapshot
import id.usecase.backend.domain.note.NoteReactionRepository
import javax.sql.DataSource

class PostgresNoteReactionRepository(
    private val dataSource: DataSource,
) : NoteReactionRepository {
    override suspend fun addLove(noteId: String, userId: String): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(INSERT_LOVE_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.setString(2, userId)
                statement.setString(3, LOVE_REACTION_TYPE)
                statement.setLong(4, System.currentTimeMillis())
                return statement.executeUpdate() == 1
            }
        }
    }

    override suspend fun removeLove(noteId: String, userId: String): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(DELETE_LOVE_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.setString(2, userId)
                statement.setString(3, LOVE_REACTION_TYPE)
                return statement.executeUpdate() == 1
            }
        }
    }

    override suspend fun hasLoved(noteId: String, userId: String): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(HAS_LOVE_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.setString(2, userId)
                statement.setString(3, LOVE_REACTION_TYPE)
                statement.executeQuery().use { resultSet ->
                    return resultSet.next()
                }
            }
        }
    }

    override suspend fun countLoves(noteId: String): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement(COUNT_LOVES_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.setString(2, LOVE_REACTION_TYPE)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) {
                        resultSet.getInt("love_count")
                    } else {
                        0
                    }
                }
            }
        }
    }

    override suspend fun getLoveSnapshot(noteId: String, userId: String): NoteLoveSnapshot {
        dataSource.connection.use { connection ->
            connection.prepareStatement(GET_LOVE_SNAPSHOT_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.setString(2, userId)
                statement.setString(3, LOVE_REACTION_TYPE)
                statement.setString(4, noteId)
                statement.setString(5, LOVE_REACTION_TYPE)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return NoteLoveSnapshot(
                            loveCount = resultSet.getInt("love_count"),
                            hasLovedByMe = resultSet.getBoolean("has_loved_by_me"),
                        )
                    }
                }
            }
        }

        return NoteLoveSnapshot(
            loveCount = 0,
            hasLovedByMe = false,
        )
    }
}

private const val LOVE_REACTION_TYPE = "LOVE"

private const val INSERT_LOVE_SQL = """
    INSERT INTO note_reactions (note_id, user_id, reaction_type, created_at_epoch_millis)
    VALUES (?, ?, ?, ?)
    ON CONFLICT (note_id, user_id, reaction_type) DO NOTHING
"""

private const val DELETE_LOVE_SQL = """
    DELETE FROM note_reactions
    WHERE note_id = ? AND user_id = ? AND reaction_type = ?
"""

private const val HAS_LOVE_SQL = """
    SELECT 1
    FROM note_reactions
    WHERE note_id = ? AND user_id = ? AND reaction_type = ?
    LIMIT 1
"""

private const val COUNT_LOVES_SQL = """
    SELECT COUNT(*)::INT AS love_count
    FROM note_reactions
    WHERE note_id = ? AND reaction_type = ?
"""

private const val GET_LOVE_SNAPSHOT_SQL = """
    SELECT
        COALESCE(aggregate.love_count, 0) AS love_count,
        EXISTS (
            SELECT 1
            FROM note_reactions mine
            WHERE mine.note_id = ? AND mine.user_id = ? AND mine.reaction_type = ?
        ) AS has_loved_by_me
    FROM (
        SELECT COUNT(*)::INT AS love_count
        FROM note_reactions
        WHERE note_id = ? AND reaction_type = ?
    ) aggregate
"""
