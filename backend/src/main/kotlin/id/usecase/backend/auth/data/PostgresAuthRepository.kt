package id.usecase.backend.auth.data

import id.usecase.backend.auth.domain.AuthRepository
import id.usecase.backend.auth.domain.AuthUser
import id.usecase.backend.auth.domain.UserStatistics
import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID
import javax.sql.DataSource

class PostgresAuthRepository(
    private val dataSource: DataSource,
) : AuthRepository {
    override suspend fun createUser(username: String, passwordHash: String, createdAtEpochMillis: Long): AuthUser {
        val normalizedUsername = username.lowercase()
        val user = AuthUser(
            userId = UUID.randomUUID().toString(),
            username = normalizedUsername,
            passwordHash = passwordHash,
            createdAtEpochMillis = createdAtEpochMillis,
        )

        dataSource.connection.use { connection ->
            connection.prepareStatement(INSERT_USER_SQL).use { statement ->
                statement.setString(1, user.userId)
                statement.setString(2, user.username)
                statement.setString(3, user.passwordHash)
                statement.setLong(4, user.createdAtEpochMillis)

                try {
                    statement.executeUpdate()
                } catch (error: SQLIntegrityConstraintViolationException) {
                    throw IllegalArgumentException("Username already exists")
                } catch (error: org.postgresql.util.PSQLException) {
                    if (error.sqlState == "23505") {
                        throw IllegalArgumentException("Username already exists")
                    }
                    throw error
                }
            }
        }

        return user
    }

    override suspend fun findByUsername(username: String): AuthUser? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_BY_USERNAME_SQL).use { statement ->
                statement.setString(1, username.lowercase())
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) {
                        resultSet.toAuthUser()
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun findById(userId: String): AuthUser? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(SELECT_BY_ID_SQL).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) {
                        resultSet.toAuthUser()
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun updatePasswordHashByUsername(username: String, passwordHash: String): AuthUser {
        dataSource.connection.use { connection ->
            connection.prepareStatement(UPDATE_PASSWORD_HASH_SQL).use { statement ->
                statement.setString(1, passwordHash)
                statement.setString(2, username.lowercase())
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.toAuthUser()
                    }
                }
            }
        }

        throw IllegalArgumentException("Username tidak ditemukan")
    }

    override suspend fun updateProfile(
        userId: String,
        displayName: String?,
        bio: String?,
        profilePictureUrl: String?,
        email: String?,
    ): AuthUser {
        val currentTime = System.currentTimeMillis()
        dataSource.connection.use { connection ->
            connection.prepareStatement(UPDATE_PROFILE_SQL).use { statement ->
                statement.setString(1, displayName)
                statement.setString(2, bio)
                statement.setString(3, profilePictureUrl)
                statement.setString(4, email)
                statement.setLong(5, currentTime)
                statement.setString(6, userId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.toAuthUser()
                    }
                }
            }
        }
        throw IllegalArgumentException("User tidak ditemukan")
    }

    override suspend fun updateLastLogin(userId: String): AuthUser {
        val currentTime = System.currentTimeMillis()
        dataSource.connection.use { connection ->
            connection.prepareStatement(UPDATE_LAST_LOGIN_SQL).use { statement ->
                statement.setLong(1, currentTime)
                statement.setString(2, userId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.toAuthUser()
                    }
                }
            }
        }
        throw IllegalArgumentException("User tidak ditemukan")
    }

    override suspend fun getUserStatistics(userId: String): UserStatistics {
        dataSource.connection.use { connection ->
            val totalNotes = connection.prepareStatement(COUNT_NOTES_SQL).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }

            val notesShared = connection.prepareStatement(COUNT_SHARED_NOTES_SQL).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }

            val notesReceived = connection.prepareStatement(COUNT_RECEIVED_NOTES_SQL).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }

            val lastSyncAt = connection.prepareStatement(SELECT_LAST_SYNC_SQL).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { rs ->
                    if (rs.next() && rs.getObject(1) != null) {
                        rs.getLong(1)
                    } else {
                        null
                    }
                }
            }

            return UserStatistics(
                totalNotes = totalNotes,
                notesShared = notesShared,
                notesReceived = notesReceived,
                lastSyncAtEpochMillis = lastSyncAt,
            )
        }
    }
}

private fun java.sql.ResultSet.toAuthUser(): AuthUser {
    return AuthUser(
        userId = getString("user_id"),
        username = getString("username"),
        passwordHash = getString("password_hash"),
        createdAtEpochMillis = getLong("created_at_epoch_millis"),
        displayName = getString("display_name"),
        bio = getString("bio"),
        profilePictureUrl = getString("profile_picture_url"),
        email = getString("email"),
        lastLoginAtEpochMillis = getLong("last_login_at_epoch_millis").takeIf { wasNull().not() },
        updatedAtEpochMillis = getLong("updated_at_epoch_millis"),
    )
}

private const val INSERT_USER_SQL = """
    INSERT INTO users (user_id, username, password_hash, created_at_epoch_millis)
    VALUES (?, ?, ?, ?)
"""

private const val SELECT_BY_USERNAME_SQL = """
    SELECT user_id, username, password_hash, created_at_epoch_millis,
           display_name, bio, profile_picture_url, email,
           last_login_at_epoch_millis, updated_at_epoch_millis
    FROM users
    WHERE username = ?
    LIMIT 1
"""

private const val SELECT_BY_ID_SQL = """
    SELECT user_id, username, password_hash, created_at_epoch_millis,
           display_name, bio, profile_picture_url, email,
           last_login_at_epoch_millis, updated_at_epoch_millis
    FROM users
    WHERE user_id = ?
    LIMIT 1
"""

private const val UPDATE_PASSWORD_HASH_SQL = """
    UPDATE users
    SET password_hash = ?,
        updated_at_epoch_millis = EXTRACT(EPOCH FROM NOW()) * 1000
    WHERE username = ?
    RETURNING user_id, username, password_hash, created_at_epoch_millis,
              display_name, bio, profile_picture_url, email,
              last_login_at_epoch_millis, updated_at_epoch_millis
"""

private const val UPDATE_PROFILE_SQL = """
    UPDATE users
    SET display_name = ?,
        bio = ?,
        profile_picture_url = ?,
        email = ?,
        updated_at_epoch_millis = ?
    WHERE user_id = ?
    RETURNING user_id, username, password_hash, created_at_epoch_millis,
              display_name, bio, profile_picture_url, email,
              last_login_at_epoch_millis, updated_at_epoch_millis
"""

private const val UPDATE_LAST_LOGIN_SQL = """
    UPDATE users
    SET last_login_at_epoch_millis = ?,
        updated_at_epoch_millis = ?
    WHERE user_id = ?
    RETURNING user_id, username, password_hash, created_at_epoch_millis,
              display_name, bio, profile_picture_url, email,
              last_login_at_epoch_millis, updated_at_epoch_millis
"""

private const val COUNT_NOTES_SQL = """
    SELECT COUNT(*)
    FROM notes
    WHERE owner_user_id = ? AND deleted_at_epoch_millis IS NULL
"""

private const val COUNT_SHARED_NOTES_SQL = """
    SELECT COUNT(DISTINCT ns.note_id)
    FROM note_shares ns
    JOIN notes n ON ns.note_id = n.id
    WHERE n.owner_user_id = ?
"""

private const val COUNT_RECEIVED_NOTES_SQL = """
    SELECT COUNT(*)
    FROM note_shares
    WHERE recipient_user_id = ?
"""

private const val SELECT_LAST_SYNC_SQL = """
    SELECT MAX(created_at_epoch_millis)
    FROM note_sync_events
    WHERE owner_user_id = ?
"""
