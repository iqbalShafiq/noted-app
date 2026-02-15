package id.usecase.backend.auth.data

import id.usecase.backend.auth.domain.AuthRepository
import id.usecase.backend.auth.domain.AuthUser
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
}

private fun java.sql.ResultSet.toAuthUser(): AuthUser {
    return AuthUser(
        userId = getString("user_id"),
        username = getString("username"),
        passwordHash = getString("password_hash"),
        createdAtEpochMillis = getLong("created_at_epoch_millis"),
    )
}

private const val INSERT_USER_SQL = """
    INSERT INTO users (user_id, username, password_hash, created_at_epoch_millis)
    VALUES (?, ?, ?, ?)
"""

private const val SELECT_BY_USERNAME_SQL = """
    SELECT user_id, username, password_hash, created_at_epoch_millis
    FROM users
    WHERE username = ?
    LIMIT 1
"""

private const val SELECT_BY_ID_SQL = """
    SELECT user_id, username, password_hash, created_at_epoch_millis
    FROM users
    WHERE user_id = ?
    LIMIT 1
"""
