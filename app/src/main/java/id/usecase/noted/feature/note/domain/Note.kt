package id.usecase.noted.feature.note.domain

import id.usecase.noted.feature.note.data.sync.LocalSyncStatus

data class Note(
    val id: Long,
    val noteId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val ownerUserId: String?,
    val syncStatus: LocalSyncStatus,
)
