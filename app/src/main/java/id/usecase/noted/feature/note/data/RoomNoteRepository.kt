package id.usecase.noted.feature.note.data

import android.util.Log
import id.usecase.noted.feature.note.data.local.NoteDao
import id.usecase.noted.feature.note.data.local.NoteEntity
import id.usecase.noted.feature.note.data.local.SyncCursorDao
import id.usecase.noted.feature.note.data.local.SyncCursorEntity
import id.usecase.noted.feature.note.data.sync.AuthApi
import id.usecase.noted.feature.note.data.sync.LocalSyncStatus
import id.usecase.noted.feature.note.data.sync.NetworkMonitor
import id.usecase.noted.feature.note.data.sync.NoteSyncApi
import id.usecase.noted.feature.note.data.sync.NoteSyncCoordinator
import id.usecase.noted.feature.note.data.sync.NoteSyncStatus
import id.usecase.noted.feature.note.data.sync.SessionStore
import id.usecase.noted.feature.note.data.sync.UserSession
import id.usecase.noted.feature.note.domain.Note
import id.usecase.noted.shared.note.SyncMutationDto
import id.usecase.noted.shared.note.SyncMutationType
import id.usecase.noted.shared.note.SyncPushRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomNoteRepository(
    private val noteDao: NoteDao,
    private val syncCursorDao: SyncCursorDao,
    private val syncApi: NoteSyncApi,
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val networkMonitor: NetworkMonitor,
    private val appScope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) : NoteRepository, NoteSyncCoordinator {
    private val syncMutex = Mutex()

    private val _session = MutableStateFlow(UserSession(userId = null, username = null, accessToken = null, deviceId = ""))
    override val session: StateFlow<UserSession> = _session.asStateFlow()

    private val _syncStatus = MutableStateFlow(NoteSyncStatus())
    override val syncStatus: StateFlow<NoteSyncStatus> = _syncStatus.asStateFlow()

    init {
        appScope.launch {
            sessionStore.ensureInitialized()

            combine(sessionStore.session, networkMonitor.isOnline) { session, isOnline ->
                session to isOnline
            }.collect { (session, isOnline) ->
                _session.value = session
                _syncStatus.update { current ->
                    current.copy(
                        userId = session.userId,
                        username = session.username,
                        isOnline = isOnline,
                    )
                }

                if (session.userId == null) {
                    val anonymousPending = noteDao.countAnonymousLocalOnly()
                    _syncStatus.update { current ->
                        current.copy(
                            pendingUploadCount = anonymousPending,
                            uploadedCount = 0,
                            totalToUpload = anonymousPending,
                        )
                    }
                    return@collect
                }

                noteDao.assignAnonymousNotesToOwner(session.userId)
                refreshPendingCount(session.userId)

                if (isOnline) {
                    syncNowInternal(
                        mode = SyncRunMode.FULL,
                        resetCursor = false,
                        source = "auto",
                    )
                }
            }
        }
    }

    override fun observeNotes(): Flow<List<Note>> {
        return noteDao.observeAllNotes().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getNoteById(noteId: Long): Note? {
        return noteDao.getNoteById(noteId)?.toDomain()
    }

    override suspend fun addNote(content: String): Note {
        val normalizedContent = content.trim()
        require(normalizedContent.isNotBlank()) { "Isi note tidak boleh kosong" }

        val now = clock()
        val currentSession = sessionStore.currentSession()
        val syncState = if (currentSession.userId == null) {
            LocalSyncStatus.LOCAL_ONLY
        } else {
            LocalSyncStatus.PENDING_UPSERT
        }

        val localId = noteDao.insert(
            NoteEntity(
                noteId = idGenerator(),
                content = normalizedContent,
                createdAt = now,
                updatedAt = now,
                ownerUserId = currentSession.userId,
                syncStatus = syncState.name,
            ),
        )

        val note = noteDao.getNoteById(localId)?.toDomain()
            ?: error("Failed to read inserted note")

        triggerAutoSyncIfPossible(currentSession)
        return note
    }

    override suspend fun updateNote(noteId: Long, content: String): Note? {
        val current = noteDao.getNoteById(noteId) ?: return null
        val normalizedContent = content.trim()
        require(normalizedContent.isNotBlank()) { "Isi note tidak boleh kosong" }

        val now = clock()
        val currentSession = sessionStore.currentSession()
        val ownerUserId = current.ownerUserId ?: currentSession.userId
        val syncState = if (ownerUserId == null) {
            LocalSyncStatus.LOCAL_ONLY
        } else {
            LocalSyncStatus.PENDING_UPSERT
        }

        val updatedRows = noteDao.updateContent(
            noteId = noteId,
            content = normalizedContent,
            updatedAt = now,
            ownerUserId = ownerUserId,
            syncStatus = syncState.name,
        )
        if (updatedRows == 0) {
            return null
        }

        val updated = noteDao.getNoteById(noteId)?.toDomain()
        triggerAutoSyncIfPossible(currentSession)
        return updated
    }

    override suspend fun deleteNote(noteId: Long): Boolean {
        val current = noteDao.getNoteById(noteId) ?: return false
        val now = clock()
        val currentSession = sessionStore.currentSession()

        if (current.ownerUserId == null && current.serverVersion == null) {
            noteDao.deleteByLocalId(noteId)
            _syncStatus.update { status ->
                status.copy(lastEventMessage = "Note lokal dihapus")
            }
            return true
        }

        val ownerUserId = current.ownerUserId ?: currentSession.userId
        if (ownerUserId == null) {
            noteDao.deleteByLocalId(noteId)
            return true
        }

        val markedRows = noteDao.markPendingDelete(
            noteId = noteId,
            updatedAt = now,
            deletedAt = now,
            ownerUserId = ownerUserId,
            syncStatus = LocalSyncStatus.PENDING_DELETE.name,
        )
        if (markedRows == 0) {
            return false
        }

        triggerAutoSyncIfPossible(currentSession)
        return true
    }

    override suspend fun register(username: String, password: String) {
        authenticate(
            source = "register",
            action = { authApi.register(username = username.trim(), password = password) },
        )
    }

    override suspend fun login(username: String, password: String) {
        authenticate(
            source = "login",
            action = { authApi.login(username = username.trim(), password = password) },
        )
    }

    override suspend fun signOut() {
        sessionStore.signOut()
        _syncStatus.update { current ->
            current.copy(
                userId = null,
                username = null,
                isSyncing = false,
                pendingUploadCount = 0,
                uploadedCount = 0,
                totalToUpload = 0,
                lastErrorMessage = null,
                lastEventMessage = "Logout berhasil",
            )
        }
    }

    override suspend fun syncNow() {
        syncNowInternal(
            mode = SyncRunMode.FULL,
            resetCursor = false,
            source = "manual-sync",
        )
    }

    override suspend fun uploadPendingNow() {
        syncNowInternal(
            mode = SyncRunMode.UPLOAD_ONLY,
            resetCursor = false,
            source = "manual-upload",
        )
    }

    override suspend fun importNow() {
        syncNowInternal(
            mode = SyncRunMode.IMPORT_ONLY,
            resetCursor = true,
            source = "manual-import",
        )
    }

    private suspend fun authenticate(
        source: String,
        action: suspend () -> id.usecase.noted.shared.auth.AuthResponse,
    ) {
        if (!networkMonitor.isCurrentlyOnline()) {
            throw IllegalStateException("Tidak ada koneksi internet")
        }

        val response = executeWithRetry(
            operationName = source,
            action = action,
        )

        sessionStore.saveAuthenticatedSession(
            userId = response.userId,
            username = response.username,
            accessToken = response.accessToken,
        )

        _syncStatus.update { current ->
            current.copy(
                userId = response.userId,
                username = response.username,
                lastErrorMessage = null,
                lastEventMessage = "Login sebagai ${response.username} berhasil",
            )
        }

        syncNowInternal(
            mode = SyncRunMode.FULL,
            resetCursor = false,
            source = source,
        )
    }

    private suspend fun triggerAutoSyncIfPossible(currentSession: UserSession) {
        if (currentSession.userId == null) {
            val pendingLocalOnly = noteDao.countAnonymousLocalOnly()
            _syncStatus.update { status ->
                status.copy(
                    pendingUploadCount = pendingLocalOnly,
                    totalToUpload = pendingLocalOnly,
                )
            }
            return
        }

        refreshPendingCount(currentSession.userId)
        if (!networkMonitor.isCurrentlyOnline()) {
            return
        }

        appScope.launch {
            syncNowInternal(
                mode = SyncRunMode.FULL,
                resetCursor = false,
                source = "local-change",
            )
        }
    }

    private suspend fun syncNowInternal(
        mode: SyncRunMode,
        resetCursor: Boolean,
        source: String,
    ) {
        val activeSession = sessionStore.currentSession()
        val userId = activeSession.userId
        val accessToken = activeSession.accessToken
        if (userId == null || accessToken.isNullOrBlank()) {
            _syncStatus.update { current ->
                current.copy(lastErrorMessage = "Login diperlukan untuk sinkronisasi")
            }
            return
        }

        if (!networkMonitor.isCurrentlyOnline()) {
            _syncStatus.update { current ->
                current.copy(
                    isOnline = false,
                    lastErrorMessage = "Tidak ada koneksi internet",
                )
            }
            return
        }

        syncMutex.withLock {
            val pendingBeforeSync = noteDao.countPendingMutations(userId)
            _syncStatus.update { current ->
                current.copy(
                    isSyncing = true,
                    userId = userId,
                    username = activeSession.username,
                    isOnline = true,
                    uploadedCount = 0,
                    totalToUpload = pendingBeforeSync,
                    pendingUploadCount = pendingBeforeSync,
                    lastErrorMessage = null,
                    lastEventMessage = "Sinkronisasi dimulai ($source)",
                )
            }

            runCatching {
                if (mode != SyncRunMode.IMPORT_ONLY) {
                    val pendingNotes = noteDao.getPendingMutations(userId)
                    pendingNotes.forEachIndexed { index, note ->
                        pushSingleMutation(
                            note = note,
                            session = activeSession,
                            userId = userId,
                            accessToken = accessToken,
                        )
                        _syncStatus.update { current ->
                            current.copy(
                                uploadedCount = index + 1,
                                pendingUploadCount = (pendingNotes.size - (index + 1)).coerceAtLeast(0),
                                lastEventMessage = "Upload ${index + 1}/${pendingNotes.size}",
                            )
                        }
                    }
                }

                if (mode != SyncRunMode.UPLOAD_ONLY) {
                    val cursor = if (resetCursor) {
                        0L
                    } else {
                        syncCursorDao.getCursor(userId) ?: 0L
                    }
                    val pullResponse = executeWithRetry(operationName = "sync-pull") {
                        syncApi.pull(
                            accessToken = accessToken,
                            cursor = cursor,
                        )
                    }
                    applyPulledNotes(userId = userId, notes = pullResponse.notes)
                    syncCursorDao.upsertCursor(
                        SyncCursorEntity(
                            userId = userId,
                            cursor = pullResponse.cursor,
                        ),
                    )
                }

                refreshPendingCount(userId)
                _syncStatus.update { current ->
                    current.copy(
                        isSyncing = false,
                        lastSyncAtEpochMillis = clock(),
                        lastErrorMessage = null,
                        lastEventMessage = "Sinkronisasi selesai",
                    )
                }
            }.onFailure { error ->
                refreshPendingCount(userId)
                val message = "Sinkronisasi gagal ($source): ${error.message}"
                Log.e(TAG, message, error)
                _syncStatus.update { current ->
                    current.copy(
                        isSyncing = false,
                        lastErrorMessage = message,
                        lastEventMessage = "Sinkronisasi gagal",
                    )
                }
            }
        }
    }

    private suspend fun pushSingleMutation(
        note: NoteEntity,
        session: UserSession,
        userId: String,
        accessToken: String,
    ) {
        val mutationType = if (note.deletedAt != null) {
            SyncMutationType.DELETE
        } else {
            SyncMutationType.UPSERT
        }

        val mutation = SyncMutationDto(
            operationId = "${session.deviceId}:${note.noteId}:${mutationType.name}:${note.updatedAt}",
            noteId = note.noteId,
            type = mutationType,
            content = if (mutationType == SyncMutationType.UPSERT) note.content else null,
            clientUpdatedAtEpochMillis = note.updatedAt,
            baseVersion = note.serverVersion,
        )

        val response = executeWithRetry(operationName = "sync-push:${note.noteId}") {
            syncApi.push(
                accessToken = accessToken,
                request = SyncPushRequest(
                    deviceId = session.deviceId,
                    mutations = listOf(mutation),
                ),
            )
        }

        val conflict = response.conflicts.firstOrNull { it.operationId == mutation.operationId }
        if (conflict != null) {
            noteDao.markSyncErrorByLocalId(note.id, conflict.reason)
            return
        }

        val applied = response.appliedNotes.firstOrNull { it.noteId == note.noteId }
        if (applied != null) {
            if (applied.deletedAtEpochMillis != null) {
                noteDao.deleteByLocalId(note.id)
            } else {
                noteDao.markSyncedByLocalId(
                    localId = note.id,
                    ownerUserId = userId,
                    content = applied.content,
                    updatedAt = applied.updatedAtEpochMillis,
                    deletedAt = applied.deletedAtEpochMillis,
                    serverVersion = applied.version,
                )
            }
        }

        val currentCursor = syncCursorDao.getCursor(userId) ?: 0L
        if (response.cursor > currentCursor) {
            syncCursorDao.upsertCursor(SyncCursorEntity(userId = userId, cursor = response.cursor))
        }
    }

    private suspend fun applyPulledNotes(
        userId: String,
        notes: List<id.usecase.noted.shared.note.SyncedNoteDto>,
    ) {
        notes.forEach { remote ->
            val local = noteDao.getByRemoteId(remote.noteId)

            if (remote.deletedAtEpochMillis != null) {
                if (local == null) {
                    return@forEach
                }

                val localStatus = local.syncStatus.toLocalSyncStatus()
                val shouldKeepLocal =
                    (localStatus == LocalSyncStatus.PENDING_UPSERT || localStatus == LocalSyncStatus.SYNC_ERROR) &&
                        local.updatedAt > remote.updatedAtEpochMillis

                if (!shouldKeepLocal) {
                    noteDao.deleteByLocalId(local.id)
                }
                return@forEach
            }

            if (local == null) {
                noteDao.insert(
                    NoteEntity(
                        noteId = remote.noteId,
                        content = remote.content,
                        createdAt = remote.createdAtEpochMillis,
                        updatedAt = remote.updatedAtEpochMillis,
                        ownerUserId = userId,
                        syncStatus = LocalSyncStatus.SYNCED.name,
                        serverVersion = remote.version,
                        deletedAt = remote.deletedAtEpochMillis,
                    ),
                )
                return@forEach
            }

            val localStatus = local.syncStatus.toLocalSyncStatus()
            val localPending =
                localStatus == LocalSyncStatus.PENDING_UPSERT ||
                    localStatus == LocalSyncStatus.PENDING_DELETE ||
                    localStatus == LocalSyncStatus.SYNC_ERROR
            if (localPending && local.updatedAt > remote.updatedAtEpochMillis) {
                return@forEach
            }

            noteDao.updateFromServerByLocalId(
                localId = local.id,
                ownerUserId = userId,
                content = remote.content,
                updatedAt = remote.updatedAtEpochMillis,
                deletedAt = remote.deletedAtEpochMillis,
                syncStatus = LocalSyncStatus.SYNCED.name,
                serverVersion = remote.version,
                syncErrorMessage = null,
            )
        }
    }

    private suspend fun refreshPendingCount(userId: String) {
        val pending = noteDao.countPendingMutations(userId)
        _syncStatus.update { current ->
            current.copy(
                pendingUploadCount = pending,
                totalToUpload = if (current.isSyncing) current.totalToUpload else pending,
            )
        }
    }

    private suspend fun <T> executeWithRetry(
        operationName: String,
        maxAttempts: Int = 4,
        initialDelayMillis: Long = 300,
        maxDelayMillis: Long = 2_500,
        action: suspend () -> T,
    ): T {
        var attempt = 0
        var delayMillis = initialDelayMillis
        var lastError: Throwable? = null

        while (attempt < maxAttempts) {
            attempt += 1
            try {
                return action()
            } catch (error: Throwable) {
                lastError = error
                val retryable = isRetryable(error)
                if (!retryable || attempt >= maxAttempts) {
                    throw error
                }

                Log.w(TAG, "retry operation=$operationName attempt=$attempt reason=${error.message}")
                delay(delayMillis)
                delayMillis = (delayMillis * 2).coerceAtMost(maxDelayMillis)
            }
        }

        throw (lastError ?: IllegalStateException("Retry failed"))
    }

    private fun isRetryable(error: Throwable): Boolean {
        return when (error) {
            is IOException -> true
            is ServerResponseException -> error.response.status.value >= 500
            is ClientRequestException -> false
            else -> false
        }
    }

    private companion object {
        const val TAG = "RoomNoteRepository"
    }
}

private enum class SyncRunMode {
    FULL,
    UPLOAD_ONLY,
    IMPORT_ONLY,
}

private fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        noteId = noteId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.toLocalSyncStatus(),
    )
}

private fun String.toLocalSyncStatus(): LocalSyncStatus {
    return LocalSyncStatus.entries.firstOrNull { it.name == this } ?: LocalSyncStatus.SYNC_ERROR
}
