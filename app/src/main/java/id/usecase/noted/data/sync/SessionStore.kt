package id.usecase.noted.data.sync

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "noted_session")

class SessionStore(
    context: Context,
) {
    private val dataStore = context.sessionDataStore

    val session: Flow<UserSession> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            UserSession(
                userId = preferences[USER_ID_KEY],
                username = preferences[USERNAME_KEY],
                accessToken = preferences[ACCESS_TOKEN_KEY],
                deviceId = preferences[DEVICE_ID_KEY].orEmpty(),
            )
        }
        .distinctUntilChanged()

    suspend fun ensureInitialized() {
        val current = dataStore.data.first()
        if (!current[DEVICE_ID_KEY].isNullOrBlank()) {
            return
        }

        dataStore.edit { preferences ->
            if (preferences[DEVICE_ID_KEY].isNullOrBlank()) {
                preferences[DEVICE_ID_KEY] = UUID.randomUUID().toString()
            }
        }
    }

    suspend fun saveAuthenticatedSession(
        userId: String,
        username: String,
        accessToken: String,
    ) {
        val normalizedUserId = userId.trim()
        val normalizedUsername = username.trim().lowercase()
        val normalizedToken = accessToken.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedUsername.isNotBlank()) { "username must not be blank" }
        require(normalizedToken.isNotBlank()) { "accessToken must not be blank" }

        ensureInitialized()
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = normalizedUserId
            preferences[USERNAME_KEY] = normalizedUsername
            preferences[ACCESS_TOKEN_KEY] = normalizedToken
        }
    }

    suspend fun signOut() {
        ensureInitialized()
        dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(ACCESS_TOKEN_KEY)
        }
    }

    suspend fun currentSession(): UserSession {
        ensureInitialized()
        return dataStore.data
            .map { preferences ->
                UserSession(
                    userId = preferences[USER_ID_KEY],
                    username = preferences[USERNAME_KEY],
                    accessToken = preferences[ACCESS_TOKEN_KEY],
                    deviceId = preferences[DEVICE_ID_KEY].orEmpty(),
                )
            }
            .first()
    }

    private companion object {
        val USER_ID_KEY: Preferences.Key<String> = stringPreferencesKey("user_id")
        val USERNAME_KEY: Preferences.Key<String> = stringPreferencesKey("username")
        val ACCESS_TOKEN_KEY: Preferences.Key<String> = stringPreferencesKey("access_token")
        val DEVICE_ID_KEY: Preferences.Key<String> = stringPreferencesKey("device_id")
    }
}
