package id.usecase

import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.sql.DataSource

data class AuthUser(
    val userId: String,
    val username: String,
    val passwordHash: String,
    val createdAtEpochMillis: Long,
)

interface AuthRepository {
    suspend fun createUser(username: String, passwordHash: String, createdAtEpochMillis: Long): AuthUser

    suspend fun findByUsername(username: String): AuthUser?

    suspend fun findById(userId: String): AuthUser?
}

class InMemoryAuthRepository : AuthRepository {
    private val usersById = linkedMapOf<String, AuthUser>()
    private val userIdByUsername = linkedMapOf<String, String>()
    private val mutex = Mutex()

    override suspend fun createUser(username: String, passwordHash: String, createdAtEpochMillis: Long): AuthUser {
        return mutex.withLock {
            val normalizedUsername = username.lowercase()
            require(normalizedUsername.isNotBlank()) { "username must not be blank" }
            if (userIdByUsername.containsKey(normalizedUsername)) {
                throw IllegalArgumentException("Username already exists")
            }

            val user = AuthUser(
                userId = UUID.randomUUID().toString(),
                username = normalizedUsername,
                passwordHash = passwordHash,
                createdAtEpochMillis = createdAtEpochMillis,
            )
            usersById[user.userId] = user
            userIdByUsername[normalizedUsername] = user.userId
            user
        }
    }

    override suspend fun findByUsername(username: String): AuthUser? {
        return mutex.withLock {
            val normalizedUsername = username.lowercase()
            val userId = userIdByUsername[normalizedUsername] ?: return@withLock null
            usersById[userId]
        }
    }

    override suspend fun findById(userId: String): AuthUser? {
        return mutex.withLock {
            usersById[userId]
        }
    }
}

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
