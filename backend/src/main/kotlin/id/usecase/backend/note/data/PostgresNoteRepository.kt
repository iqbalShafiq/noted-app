package id.usecase.backend.note.data

import id.usecase.backend.note.domain.NoteRepository
import id.usecase.backend.note.domain.NoteSyncRepository
import id.usecase.backend.note.domain.NoteVisibility
import id.usecase.backend.note.domain.StoredNote
import id.usecase.backend.note.domain.SyncApplyResult
import id.usecase.backend.note.domain.SyncApplyStatus
import id.usecase.backend.note.domain.SyncPullData
import id.usecase.noted.shared.note.SyncMutationDto
import id.usecase.noted.shared.note.SyncMutationType
import javax.sql.DataSource

class PostgresNoteRepository(
    private val dataSource: DataSource,
) : NoteRepository, NoteSyncRepository {
    override suspend fun create(note: StoredNote): StoredNote {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(INSERT_SQL).use { statement ->
                    statement.setString(1, note.id)
                    statement.setString(2, note.ownerUserId)
                    statement.setString(3, note.content)
                    statement.setLong(4, note.createdAtEpochMillis)
                    statement.setLong(5, note.updatedAtEpochMillis)
                    statement.setObject(6, note.deletedAtEpochMillis)
                    statement.setLong(7, note.version)
                    statement.setString(8, note.visibility.name)
                    statement.executeUpdate()
                }

                connection.prepareStatement(INSERT_SYNC_EVENT_SQL).use { statement ->
                    statement.setString(1, note.ownerUserId)
                    statement.setString(2, "legacy-create:${note.id}:${note.updatedAtEpochMillis}")
                    statement.setString(3, note.id)
                    statement.setLong(4, note.updatedAtEpochMillis)
                    statement.executeUpdate()
                }

                connection.commit()
                return note
            } catch (error: Exception) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    override suspend fun findById(noteId: String): StoredNote? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_BY_ID_SQL).use { statement ->
                statement.setString(1, noteId)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) {
                        resultSet.toStoredNote()
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun findByOwner(ownerUserId: String): List<StoredNote> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_BY_OWNER_SQL).use { statement ->
                statement.setString(1, ownerUserId)
                statement.executeQuery().use { resultSet ->
                    val notes = mutableListOf<StoredNote>()
                    while (resultSet.next()) {
                        notes += resultSet.toStoredNote()
                    }
                    return notes
                }
            }
        }
    }

    override suspend fun findByIds(noteIds: Set<String>): List<StoredNote> {
        if (noteIds.isEmpty()) {
            return emptyList()
        }

        val placeholders = List(noteIds.size) { "?" }.joinToString(",")
        val sql = """
            SELECT id, owner_user_id, content, created_at_epoch_millis, updated_at_epoch_millis, deleted_at_epoch_millis, version, visibility
            FROM notes
            WHERE id IN ($placeholders) AND deleted_at_epoch_millis IS NULL
            ORDER BY created_at_epoch_millis DESC
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                noteIds.forEachIndexed { index, noteId ->
                    statement.setString(index + 1, noteId)
                }

                statement.executeQuery().use { resultSet ->
                    val notes = mutableListOf<StoredNote>()
                    while (resultSet.next()) {
                        notes += resultSet.toStoredNote()
                    }
                    return notes
                }
            }
        }
    }

    override suspend fun findAllExcludingOwner(excludeOwnerUserId: String, limit: Int): List<StoredNote> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_ALL_EXCLUDING_OWNER_SQL).use { statement ->
                statement.setString(1, excludeOwnerUserId)
                statement.setInt(2, limit)
                statement.executeQuery().use { resultSet ->
                    val notes = mutableListOf<StoredNote>()
                    while (resultSet.next()) {
                        notes += resultSet.toStoredNote()
                    }
                    return notes
                }
            }
        }
    }

    override suspend fun findPublicNotes(limit: Int): List<StoredNote> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_PUBLIC_NOTES_SQL).use { statement ->
                statement.setString(1, NoteVisibility.PUBLIC.name)
                statement.setInt(2, limit)
                statement.executeQuery().use { resultSet ->
                    val notes = mutableListOf<StoredNote>()
                    while (resultSet.next()) {
                        notes += resultSet.toStoredNote()
                    }
                    return notes
                }
            }
        }
    }

    override suspend fun applyMutation(
        ownerUserId: String,
        mutation: SyncMutationDto,
        serverNowEpochMillis: Long,
    ): SyncApplyResult {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val isNewOperation = connection.prepareStatement(INSERT_SYNC_EVENT_SQL).use { statement ->
                    statement.setString(1, ownerUserId)
                    statement.setString(2, mutation.operationId)
                    statement.setString(3, mutation.noteId)
                    statement.setLong(4, serverNowEpochMillis)
                    statement.executeUpdate() == 1
                }

                if (!isNewOperation) {
                    connection.rollback()
                    return SyncApplyResult(
                        status = SyncApplyStatus.DUPLICATE,
                        appliedNote = connection.prepareStatement(SELECT_BY_ID_SQL).use { statement ->
                            statement.setString(1, mutation.noteId)
                            statement.executeQuery().use { resultSet ->
                                if (resultSet.next()) resultSet.toStoredNote() else null
                            }
                        },
                    )
                }

                val existing = connection.prepareStatement(SELECT_BY_ID_FOR_UPDATE_SQL).use { statement ->
                    statement.setString(1, mutation.noteId)
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) resultSet.toStoredNote() else null
                    }
                }

                if (existing != null && existing.ownerUserId != ownerUserId) {
                    connection.rollback()
                    return SyncApplyResult(
                        status = SyncApplyStatus.CONFLICT,
                        conflictReason = "Note belongs to another owner",
                        conflictServerNote = existing,
                    )
                }

                val expectedVersion = mutation.baseVersion ?: existing?.version
                if (existing == null && mutation.baseVersion != null && mutation.baseVersion != 0L) {
                    connection.rollback()
                    return SyncApplyResult(
                        status = SyncApplyStatus.CONFLICT,
                        conflictReason = "Note does not exist for baseVersion=${mutation.baseVersion}",
                        conflictServerNote = null,
                    )
                }
                if (expectedVersion != null && existing != null && existing.version != expectedVersion) {
                    connection.rollback()
                    return SyncApplyResult(
                        status = SyncApplyStatus.CONFLICT,
                        conflictReason = "Version conflict",
                        conflictServerNote = existing,
                    )
                }

                val effectiveUpdatedAt = maxOf(serverNowEpochMillis, mutation.clientUpdatedAtEpochMillis)

                val applied = when (mutation.type) {
                    SyncMutationType.UPSERT -> {
                        val normalizedContent = mutation.content?.trim().orEmpty()
                        require(normalizedContent.isNotBlank()) { "content must not be blank for UPSERT" }

                        if (existing == null) {
                            val created = StoredNote(
                                id = mutation.noteId,
                                ownerUserId = ownerUserId,
                                content = normalizedContent,
                                createdAtEpochMillis = effectiveUpdatedAt,
                                updatedAtEpochMillis = effectiveUpdatedAt,
                                deletedAtEpochMillis = null,
                                version = 1,
                                visibility = NoteVisibility.PRIVATE,
                            )
                            connection.prepareStatement(INSERT_SQL).use { statement ->
                                statement.setString(1, created.id)
                                statement.setString(2, created.ownerUserId)
                                statement.setString(3, created.content)
                                statement.setLong(4, created.createdAtEpochMillis)
                                statement.setLong(5, created.updatedAtEpochMillis)
                                statement.setObject(6, created.deletedAtEpochMillis)
                                statement.setLong(7, created.version)
                                statement.setString(8, created.visibility.name)
                                statement.executeUpdate()
                            }
                            created
                        } else {
                            val updated = existing.copy(
                                content = normalizedContent,
                                updatedAtEpochMillis = effectiveUpdatedAt,
                                deletedAtEpochMillis = null,
                                version = existing.version + 1,
                            )
                            connection.prepareStatement(UPDATE_SQL).use { statement ->
                                statement.setString(1, updated.content)
                                statement.setLong(2, updated.updatedAtEpochMillis)
                                statement.setObject(3, updated.deletedAtEpochMillis)
                                statement.setLong(4, updated.version)
                                statement.setString(5, updated.id)
                                statement.executeUpdate()
                            }
                            updated
                        }
                    }

                    SyncMutationType.DELETE -> {
                        if (existing == null) {
                            val deleted = StoredNote(
                                id = mutation.noteId,
                                ownerUserId = ownerUserId,
                                content = "",
                                createdAtEpochMillis = effectiveUpdatedAt,
                                updatedAtEpochMillis = effectiveUpdatedAt,
                                deletedAtEpochMillis = effectiveUpdatedAt,
                                version = 1,
                                visibility = NoteVisibility.PRIVATE,
                            )
                            connection.prepareStatement(INSERT_SQL).use { statement ->
                                statement.setString(1, deleted.id)
                                statement.setString(2, deleted.ownerUserId)
                                statement.setString(3, deleted.content)
                                statement.setLong(4, deleted.createdAtEpochMillis)
                                statement.setLong(5, deleted.updatedAtEpochMillis)
                                statement.setObject(6, deleted.deletedAtEpochMillis)
                                statement.setLong(7, deleted.version)
                                statement.setString(8, deleted.visibility.name)
                                statement.executeUpdate()
                            }
                            deleted
                        } else {
                            val deleted = existing.copy(
                                updatedAtEpochMillis = effectiveUpdatedAt,
                                deletedAtEpochMillis = effectiveUpdatedAt,
                                version = existing.version + 1,
                            )
                            connection.prepareStatement(UPDATE_SQL).use { statement ->
                                statement.setString(1, deleted.content)
                                statement.setLong(2, deleted.updatedAtEpochMillis)
                                statement.setObject(3, deleted.deletedAtEpochMillis)
                                statement.setLong(4, deleted.version)
                                statement.setString(5, deleted.id)
                                statement.executeUpdate()
                            }
                            deleted
                        }
                    }
                }

                connection.commit()
                return SyncApplyResult(
                    status = SyncApplyStatus.APPLIED,
                    appliedNote = applied,
                )
            } catch (error: Exception) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    override suspend fun pullChanges(ownerUserId: String, afterCursor: Long): SyncPullData {
        dataSource.connection.use { connection ->
            val cursor = connection.prepareStatement(SELECT_CURSOR_SQL).use { statement ->
                statement.setString(1, ownerUserId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.getLong("cursor") else 0L
                }
            }

            connection.prepareStatement(SELECT_PULL_CHANGES_SQL).use { statement ->
                statement.setString(1, ownerUserId)
                statement.setLong(2, afterCursor)
                statement.executeQuery().use { resultSet ->
                    val notes = mutableListOf<StoredNote>()
                    while (resultSet.next()) {
                        notes += resultSet.toStoredNote()
                    }

                    return SyncPullData(cursor = cursor, notes = notes)
                }
            }
        }
    }

    override suspend fun currentCursor(ownerUserId: String): Long {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_CURSOR_SQL).use { statement ->
                statement.setString(1, ownerUserId)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) {
                        resultSet.getLong("cursor")
                    } else {
                        0L
                    }
                }
            }
        }
    }
}

private fun java.sql.ResultSet.toStoredNote(): StoredNote {
    val deletedAt = getObject("deleted_at_epoch_millis") as? Number
    val visibilityStr = getString("visibility")
    val visibility = visibilityStr?.let {
        runCatching { NoteVisibility.valueOf(it) }.getOrDefault(NoteVisibility.PRIVATE)
    } ?: NoteVisibility.PRIVATE
    return StoredNote(
        id = getString("id"),
        ownerUserId = getString("owner_user_id"),
        content = getString("content"),
        createdAtEpochMillis = getLong("created_at_epoch_millis"),
        updatedAtEpochMillis = getLong("updated_at_epoch_millis"),
        deletedAtEpochMillis = deletedAt?.toLong(),
        version = getLong("version"),
        visibility = visibility,
    )
}

private const val INSERT_SQL = """
    INSERT INTO notes (
        id,
        owner_user_id,
        content,
        created_at_epoch_millis,
        updated_at_epoch_millis,
        deleted_at_epoch_millis,
        version,
        visibility
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
"""

private const val UPDATE_SQL = """
    UPDATE notes
    SET content = ?, updated_at_epoch_millis = ?, deleted_at_epoch_millis = ?, version = ?
    WHERE id = ?
"""

private const val SELECT_BY_ID_SQL = """
    SELECT id, owner_user_id, content, created_at_epoch_millis, updated_at_epoch_millis, deleted_at_epoch_millis, version, visibility
    FROM notes
    WHERE id = ?
    LIMIT 1
"""

private const val SELECT_BY_ID_FOR_UPDATE_SQL = """
    SELECT id, owner_user_id, content, created_at_epoch_millis, updated_at_epoch_millis, deleted_at_epoch_millis, version, visibility
    FROM notes
    WHERE id = ?
    FOR UPDATE
"""

private const val SELECT_BY_OWNER_SQL = """
    SELECT id, owner_user_id, content, created_at_epoch_millis, updated_at_epoch_millis, deleted_at_epoch_millis, version, visibility
    FROM notes
    WHERE owner_user_id = ? AND deleted_at_epoch_millis IS NULL
    ORDER BY created_at_epoch_millis DESC
"""

private const val SELECT_ALL_EXCLUDING_OWNER_SQL = """
    SELECT id, owner_user_id, content, created_at_epoch_millis, updated_at_epoch_millis, deleted_at_epoch_millis, version, visibility
    FROM notes
    WHERE owner_user_id != ? AND deleted_at_epoch_millis IS NULL
    ORDER BY created_at_epoch_millis DESC
    LIMIT ?
"""

private const val SELECT_PUBLIC_NOTES_SQL = """
    SELECT id, owner_user_id, content, created_at_epoch_millis, updated_at_epoch_millis, deleted_at_epoch_millis, version, visibility
    FROM notes
    WHERE visibility = ? AND deleted_at_epoch_millis IS NULL
    ORDER BY created_at_epoch_millis DESC
    LIMIT ?
"""

private const val INSERT_SYNC_EVENT_SQL = """
    INSERT INTO note_sync_events (owner_user_id, operation_id, note_id, created_at_epoch_millis)
    VALUES (?, ?, ?, ?)
    ON CONFLICT (owner_user_id, operation_id) DO NOTHING
"""

private const val SELECT_CURSOR_SQL = """
    SELECT COALESCE(MAX(cursor), 0) AS cursor
    FROM note_sync_events
    WHERE owner_user_id = ?
"""

private const val SELECT_PULL_CHANGES_SQL = """
    WITH changed AS (
        SELECT note_id, MAX(cursor) AS latest_cursor
        FROM note_sync_events
        WHERE owner_user_id = ? AND cursor > ?
        GROUP BY note_id
    )
    SELECT n.id, n.owner_user_id, n.content, n.created_at_epoch_millis, n.updated_at_epoch_millis, n.deleted_at_epoch_millis, n.version, n.visibility
    FROM changed c
    JOIN notes n ON n.id = c.note_id
    ORDER BY c.latest_cursor ASC
"""
