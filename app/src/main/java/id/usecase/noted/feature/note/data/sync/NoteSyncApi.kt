package id.usecase.noted.feature.note.data.sync

import id.usecase.noted.shared.note.SyncPullResponse
import id.usecase.noted.shared.note.SyncPushRequest
import id.usecase.noted.shared.note.SyncPushResponse

interface NoteSyncApi {
    suspend fun push(accessToken: String, request: SyncPushRequest): SyncPushResponse

    suspend fun pull(accessToken: String, cursor: Long): SyncPullResponse
}
