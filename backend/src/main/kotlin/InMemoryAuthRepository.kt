package id.usecase

import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
