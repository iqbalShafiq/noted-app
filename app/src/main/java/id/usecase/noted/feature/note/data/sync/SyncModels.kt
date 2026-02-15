package id.usecase.noted.feature.note.data.sync

data class UserSession(
    val userId: String? = null,
    val username: String? = null,
    val accessToken: String? = null,
    val deviceId: String,
) {
    val isLoggedIn: Boolean
        get() = !userId.isNullOrBlank() && !accessToken.isNullOrBlank()
}

data class NoteSyncStatus(
    val isOnline: Boolean = false,
    val userId: String? = null,
    val username: String? = null,
    val isSyncing: Boolean = false,
    val pendingUploadCount: Int = 0,
    val uploadedCount: Int = 0,
    val totalToUpload: Int = 0,
    val lastSyncAtEpochMillis: Long? = null,
    val lastErrorMessage: String? = null,
    val lastEventMessage: String? = null,
) {
    val isLoggedIn: Boolean
        get() = !userId.isNullOrBlank()
}

interface NoteSyncCoordinator {
    val session: kotlinx.coroutines.flow.StateFlow<UserSession>
    val syncStatus: kotlinx.coroutines.flow.StateFlow<NoteSyncStatus>

    suspend fun register(username: String, password: String)

    suspend fun login(username: String, password: String)

    suspend fun forgotPassword(username: String, newPassword: String)

    suspend fun signOut()

    suspend fun syncNow()

    suspend fun uploadPendingNow()

    suspend fun importNow()
}
