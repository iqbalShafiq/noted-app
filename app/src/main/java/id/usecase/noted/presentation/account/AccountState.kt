package id.usecase.noted.presentation.account

data class AccountState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val username: String? = null,
    val userId: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null,
    val email: String? = null,
    val createdAtEpochMillis: Long? = null,
    val lastLoginAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long? = null,
    val totalNotes: Int = 0,
    val notesShared: Int = 0,
    val notesReceived: Int = 0,
    val lastSyncAtEpochMillis: Long? = null,
)
