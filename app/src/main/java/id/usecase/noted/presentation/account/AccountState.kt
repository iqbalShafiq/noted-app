package id.usecase.noted.presentation.account

data class AccountState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val username: String? = null,
    val userId: String? = null,
    val lastSyncAt: Long? = null,
)
