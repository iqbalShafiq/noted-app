package id.usecase.noted.data.sync

enum class LocalSyncStatus {
    LOCAL_ONLY,
    PENDING_UPSERT,
    PENDING_DELETE,
    SYNCED,
    SYNC_ERROR,
}
