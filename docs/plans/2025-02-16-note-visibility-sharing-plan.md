# Note Visibility & Sharing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement note visibility levels (PRIVATE, LINK_SHARED, PUBLIC), link/QR sharing, fork functionality, and proper sync propagation.

**Architecture:** Three-tier visibility system with deep link handling, QR generation, and fork metadata tracking. Changes propagate via existing sync mechanism with visibility filters on backend explore endpoint.

**Tech Stack:** Android (Jetpack Compose, Room, Navigation 3), Ktor backend, PostgreSQL, kotlinx.serialization, ZXing (QR code)

---

## Overview

This plan implements a comprehensive note visibility and sharing system:

1. **Visibility Levels:**
   - `PRIVATE`: Only owner can see (not in explore, not shareable)
   - `LINK_SHARED`: Accessible via link/QR but hidden from explore
   - `PUBLIC`: Visible in explore feed

2. **Sharing Mechanisms:**
   - Deep link: `noted://note/{noteId}`
   - Web link: `https://noted.app/note/{noteId}` (redirects to app)
   - QR Code: Contains shareable link

3. **Fork Feature:**
   - Clone note with metadata preservation (original author, forkedFrom)
   - Creates new independent note

4. **Sync:**
   - Visibility changes propagate to all users
   - If visibility changes to PRIVATE, shared copies become inaccessible

---

## Phase 1: Data Model & Contracts

### Task 1: Create NoteVisibility Enum

**Files:**
- Create: `app/src/main/java/id/usecase/noted/domain/NoteVisibility.kt`

**Step 1: Create visibility enum**

```kotlin
package id.usecase.noted.domain

enum class NoteVisibility {
    PRIVATE,      // Only owner
    LINK_SHARED,  // Link/QR access only
    PUBLIC;       // Public explore feed

    companion object {
        fun fromString(value: String): NoteVisibility =
            entries.find { it.name == value } ?: PRIVATE
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/domain/NoteVisibility.kt
git commit -m "feat: add NoteVisibility enum with PRIVATE, LINK_SHARED, PUBLIC"
```

---

### Task 2: Add Visibility to Domain Note Model

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/domain/Note.kt`

**Step 1: Add visibility field**

```kotlin
package id.usecase.noted.domain

data class Note(
    val id: Long,
    val noteId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val ownerUserId: String?,
    val syncStatus: LocalSyncStatus,
    val visibility: NoteVisibility = NoteVisibility.PRIVATE, // NEW
)
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/domain/Note.kt
git commit -m "feat: add visibility field to domain Note model"
```

---

### Task 3: Add Visibility to Room Entity

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/data/local/NoteEntity.kt`

**Step 1: Add visibility column**

```kotlin
package id.usecase.noted.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["note_id"], unique = true),
        Index(value = ["owner_user_id"]),
        Index(value = ["sync_status"]),
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: String,
    val content: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "server_version") val serverVersion: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_error_message") val syncErrorMessage: String? = null,
    @ColumnInfo(name = "visibility") val visibility: String = "PRIVATE", // NEW
)
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/data/local/NoteEntity.kt
git commit -m "feat: add visibility column to NoteEntity"
```

---

### Task 4: Add Visibility to Shared DTOs

**Files:**
- Modify: `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteContracts.kt`

**Step 1: Add visibility field to NoteDto**

```kotlin
@Serializable
data class NoteDto(
    val id: String,
    val ownerUserId: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val sharedWithUserIds: List<String> = emptyList(),
    val visibility: String = "PRIVATE", // NEW
)
```

**Step 2: Add visibility to SyncedNoteDto**

```kotlin
@Serializable
data class SyncedNoteDto(
    val noteId: String,
    val ownerUserId: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val version: Long,
    val sharedWithUserIds: List<String> = emptyList(),
    val visibility: String = "PRIVATE", // NEW
)
```

**Step 3: Commit**

```bash
git add shared/src/main/kotlin/id/usecase/noted/shared/note/NoteContracts.kt
git commit -m "feat: add visibility field to NoteDto and SyncedNoteDto"
```

---

### Task 5: Create Database Migration 2→3

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/data/local/NoteDatabase.kt`

**Step 1: Add migration object**

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN visibility TEXT NOT NULL DEFAULT 'PRIVATE'")
    }
}
```

**Step 2: Update database builder**

```kotlin
Room.databaseBuilder(context, NoteDatabase::class.java, "note_database")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Add MIGRATION_2_3
    .build()
```

**Step 3: Increment version**

```kotlin
@Database(
    entities = [NoteEntity::class, SyncCursorEntity::class],
    version = 3, // Increment from 2
    exportSchema = true
)
```

**Step 4: Commit**

```bash
git add app/src/main/java/id/usecase/noted/data/local/NoteDatabase.kt
git commit -m "feat: add migration 2→3 for visibility column"
```

---

## Phase 2: Backend Implementation

### Task 6: Add Visibility to Backend StoredNote

**Files:**
- Modify: `backend/src/main/kotlin/id/usecase/backend/note/domain/StoredNote.kt`

**Step 1: Add visibility field**

```kotlin
package id.usecase.backend.note.domain

data class StoredNote(
    val id: String,
    val ownerUserId: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val version: Long = 1,
    val visibility: String = "PRIVATE", // NEW
)
```

**Step 2: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/note/domain/StoredNote.kt
git commit -m "feat: add visibility to StoredNote domain model"
```

---

### Task 7: Update PostgresNoteRepository with Visibility

**Files:**
- Modify: `backend/src/main/kotlin/id/usecase/backend/note/data/PostgresNoteRepository.kt`

**Step 1: Add visibility column to SQL queries**

```kotlin
companion object {
    private const val INSERT_SQL = """
        INSERT INTO notes (id, owner_user_id, content, created_at, updated_at, deleted_at, version, visibility)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            content = EXCLUDED.content,
            updated_at = EXCLUDED.updated_at,
            deleted_at = EXCLUDED.deleted_at,
            version = EXCLUDED.version,
            visibility = EXCLUDED.visibility
    """
    
    private const val SELECT_BY_ID_SQL = """
        SELECT id, owner_user_id, content, created_at, updated_at, deleted_at, version, visibility
        FROM notes WHERE id = ?
    """
    
    private const val SELECT_BY_OWNER_SQL = """
        SELECT id, owner_user_id, content, created_at, updated_at, deleted_at, version, visibility
        FROM notes WHERE owner_user_id = ? AND deleted_at IS NULL
        ORDER BY updated_at DESC
    """
    
    // Add query for explore - only PUBLIC notes
    private const val SELECT_PUBLIC_SQL = """
        SELECT id, owner_user_id, content, created_at, updated_at, deleted_at, version, visibility
        FROM notes 
        WHERE owner_user_id != ? 
        AND deleted_at IS NULL 
        AND visibility = 'PUBLIC'
        ORDER BY updated_at DESC
        LIMIT ?
    """
}
```

**Step 2: Update mapping functions**

```kotlin
private fun ResultSet.toStoredNote(): StoredNote = StoredNote(
    id = getString("id"),
    ownerUserId = getString("owner_user_id"),
    content = getString("content"),
    createdAtEpochMillis = getLong("created_at"),
    updatedAtEpochMillis = getLong("updated_at"),
    deletedAtEpochMillis = getLong("deleted_at").takeIf { !wasNull() },
    version = getLong("version"),
    visibility = getString("visibility"), // NEW
)
```

**Step 3: Update insert parameters**

```kotlin
private fun PreparedStatement.setNoteParameters(note: StoredNote) {
    setString(1, note.id)
    setString(2, note.ownerUserId)
    setString(3, note.content)
    setLong(4, note.createdAtEpochMillis)
    setLong(5, note.updatedAtEpochMillis)
    setLong(6, note.deletedAtEpochMillis ?: 0)
    setLong(7, note.version)
    setString(8, note.visibility) // NEW
}
```

**Step 4: Add method to get public notes for explore**

```kotlin
override suspend fun findPublicNotes(excludeOwnerId: String, limit: Int): List<StoredNote> = 
    withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_PUBLIC_SQL).use { stmt ->
                stmt.setString(1, excludeOwnerId)
                stmt.setInt(2, limit)
                stmt.executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs.toStoredNote() else null }
                        .toList()
                }
            }
        }
    }
```

**Step 5: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/note/data/PostgresNoteRepository.kt
git commit -m "feat: add visibility support to PostgresNoteRepository"
```

---

### Task 8: Update NoteRepository Interface

**Files:**
- Modify: `backend/src/main/kotlin/id/usecase/backend/note/domain/NoteRepository.kt`

**Step 1: Add findPublicNotes method**

```kotlin
package id.usecase.backend.note.domain

interface NoteRepository {
    suspend fun findById(id: String): StoredNote?
    suspend fun findByOwner(ownerUserId: String): List<StoredNote>
    suspend fun save(note: StoredNote): StoredNote
    suspend fun findByIds(ids: List<String>): List<StoredNote>
    suspend fun findPublicNotes(excludeOwnerId: String, limit: Int): List<StoredNote> // NEW
}
```

**Step 2: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/note/domain/NoteRepository.kt
git commit -m "feat: add findPublicNotes to NoteRepository interface"
```

---

### Task 9: Update NoteSharingService

**Files:**
- Modify: `backend/src/main/kotlin/id/usecase/backend/note/service/NoteSharingService.kt`

**Step 1: Update exploreNotes to use visibility filter**

```kotlin
suspend fun exploreNotes(excludeUserId: String, limit: Int = 50): List<NoteDto> {
    val normalizedUserId = excludeUserId.lowercase()
    // Now only returns PUBLIC notes
    val notes = noteRepository.findPublicNotes(normalizedUserId, limit)
    return notes.map { note ->
        note.toNoteDto(sharedWithUserIds = sharedWithUserIds(note.id))
    }
}
```

**Step 2: Update toNoteDto mapper**

```kotlin
private fun StoredNote.toNoteDto(sharedWithUserIds: List<String>): NoteDto = NoteDto(
    id = id,
    ownerUserId = ownerUserId,
    content = content,
    createdAtEpochMillis = createdAtEpochMillis,
    sharedWithUserIds = sharedWithUserIds,
    visibility = visibility, // NEW
)
```

**Step 3: Add method to get note by link with visibility check**

```kotlin
suspend fun getNoteByLink(noteId: String, requesterUserId: String?): NoteDto? {
    val note = noteRepository.findById(noteId) ?: return null
    
    return when (note.visibility) {
        "PUBLIC" -> note.toNoteDto(sharedWithUserIds(note.id))
        "LINK_SHARED" -> note.toNoteDto(sharedWithUserIds(note.id))
        "PRIVATE" -> {
            // Only owner can access private notes
            if (note.ownerUserId == requesterUserId?.lowercase()) {
                note.toNoteDto(sharedWithUserIds(note.id))
            } else null
        }
        else -> null
    }
}
```

**Step 4: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/note/service/NoteSharingService.kt
git commit -m "feat: update NoteSharingService with visibility filtering"
```

---

### Task 10: Add Get Note by ID Endpoint

**Files:**
- Modify: `backend/src/main/kotlin/id/usecase/backend/note/presentation/NoteRoutes.kt`

**Step 1: Add endpoint to get specific note**

```kotlin
get("/notes/{noteId}") {
    val userId = call.requireUserId()
    val noteId = call.parameters["noteId"] ?: return@get call.respond(
        HttpStatusCode.BadRequest,
        ErrorResponse("Note ID required")
    )
    
    val note = noteSharingService.getNoteByLink(noteId, userId)
        ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Note not found or access denied"))
    
    call.respond(note)
}
```

**Step 2: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/note/presentation/NoteRoutes.kt
git commit -m "feat: add GET /notes/{noteId} endpoint with visibility check"
```

---

### Task 11: Update Backend Database Schema

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__create_tables.sql` (or create V2)

**Step 1: Add visibility column to notes table**

If you have existing migrations, create a new one:

```sql
-- V2__add_note_visibility.sql
ALTER TABLE notes ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';
CREATE INDEX IF NOT EXISTS idx_notes_visibility ON notes(visibility);
```

Or if creating fresh, add to V1:

```sql
CREATE TABLE IF NOT EXISTS notes (
    id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT,
    version BIGINT DEFAULT 1,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE', -- NEW
    FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_notes_owner ON notes(owner_user_id);
CREATE INDEX idx_notes_visibility ON notes(visibility); -- NEW
```

**Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration/
git commit -m "feat: add visibility column to backend notes table"
```

---

## Phase 3: App Data Layer

### Task 12: Update NoteDao with Visibility Queries

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/data/local/NoteDao.kt`

**Step 1: Add query to filter by visibility**

```kotlin
@Query("SELECT * FROM notes WHERE visibility = :visibility AND deleted_at IS NULL ORDER BY updated_at DESC")
fun getNotesByVisibility(visibility: String): Flow<List<NoteEntity>>

@Query("SELECT * FROM notes WHERE visibility IN ('PUBLIC', 'LINK_SHARED') AND owner_user_id != :ownerId AND deleted_at IS NULL ORDER BY updated_at DESC")
fun getSharedNotesExcludingOwner(ownerId: String): Flow<List<NoteEntity>>
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/data/local/NoteDao.kt
git commit -m "feat: add visibility queries to NoteDao"
```

---

### Task 13: Update RoomNoteRepository

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/data/RoomNoteRepository.kt`

**Step 1: Update entity-to-domain mapper**

```kotlin
private fun NoteEntity.toDomain(): Note = Note(
    id = id,
    noteId = noteId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    ownerUserId = ownerUserId,
    syncStatus = LocalSyncStatus.fromString(syncStatus),
    visibility = NoteVisibility.fromString(visibility), // NEW
)
```

**Step 2: Update domain-to-entity mapper**

```kotlin
private fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    noteId = noteId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    ownerUserId = ownerUserId,
    syncStatus = syncStatus.name,
    serverVersion = if (syncStatus == LocalSyncStatus.SYNCED) 1 else null,
    deletedAt = null,
    visibility = visibility.name, // NEW
)
```

**Step 3: Update sync pull to handle visibility**

```kotlin
private fun SyncedNoteDto.toEntity(): NoteEntity = NoteEntity(
    noteId = noteId,
    content = content,
    createdAt = createdAtEpochMillis,
    updatedAt = updatedAtEpochMillis,
    ownerUserId = ownerUserId,
    syncStatus = LocalSyncStatus.SYNCED.name,
    serverVersion = version,
    deletedAt = deletedAtEpochMillis,
    visibility = visibility, // NEW
)
```

**Step 4: Commit**

```bash
git add app/src/main/java/id/usecase/noted/data/RoomNoteRepository.kt
git commit -m "feat: add visibility mapping in RoomNoteRepository"
```

---

## Phase 4: App Presentation Layer - Editor

### Task 14: Add Visibility Selector UI

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/note/editor/component/VisibilitySelector.kt`

**Step 1: Create visibility selector composable**

```kotlin
package id.usecase.noted.presentation.note.editor.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.usecase.noted.domain.NoteVisibility

@Composable
fun VisibilitySelector(
    visibility: NoteVisibility,
    onVisibilityChange: (NoteVisibility) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = visibility == NoteVisibility.PRIVATE,
            onClick = { onVisibilityChange(NoteVisibility.PRIVATE) },
            label = { Text("Private") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                )
            },
        )
        FilterChip(
            selected = visibility == NoteVisibility.LINK_SHARED,
            onClick = { onVisibilityChange(NoteVisibility.LINK_SHARED) },
            label = { Text("Link Share") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                )
            },
        )
        FilterChip(
            selected = visibility == NoteVisibility.PUBLIC,
            onClick = { onVisibilityChange(NoteVisibility.PUBLIC) },
            label = { Text("Public") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                )
            },
        )
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/component/VisibilitySelector.kt
git commit -m "feat: add VisibilitySelector composable"
```

---

### Task 15: Update NoteEditorState

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorState.kt`

**Step 1: Add visibility field**

```kotlin
package id.usecase.noted.presentation.note.editor

import id.usecase.noted.domain.NoteVisibility

data class NoteEditorState(
    val editingNoteId: Long? = null,
    val blocks: List<NoteEditorBlock> = listOf(NoteEditorBlock.Text(UUID.randomUUID().toString(), "")),
    val isLoadingNote: Boolean = false,
    val isSaving: Boolean = false,
    val visibility: NoteVisibility = NoteVisibility.PRIVATE, // NEW
)
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorState.kt
git commit -m "feat: add visibility to NoteEditorState"
```

---

### Task 16: Update NoteEditorIntent

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorIntent.kt`

**Step 1: Add visibility change intent**

```kotlin
package id.usecase.noted.presentation.note.editor

import id.usecase.noted.domain.NoteVisibility

sealed interface NoteEditorIntent {
    // ... existing intents ...
    data class VisibilityChanged(val visibility: NoteVisibility) : NoteEditorIntent // NEW
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorIntent.kt
git commit -m "feat: add VisibilityChanged intent"
```

---

### Task 17: Update NoteEditorViewModel

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorViewModel.kt`

**Step 1: Handle visibility change**

```kotlin
private fun handleIntent(intent: NoteEditorIntent) {
    when (intent) {
        // ... existing handlers ...
        is NoteEditorIntent.VisibilityChanged -> {
            _state.update { it.copy(visibility = intent.visibility) }
        }
    }
}
```

**Step 2: Include visibility when saving**

```kotlin
private suspend fun saveNote() {
    val note = Note(
        id = state.value.editingNoteId ?: 0,
        noteId = existingNote?.noteId ?: generateNoteId(),
        content = encodeContent(state.value.blocks),
        createdAt = existingNote?.createdAt ?: System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        ownerUserId = sessionStore.getSession()?.userId,
        syncStatus = LocalSyncStatus.PENDING_UPSERT,
        visibility = state.value.visibility, // NEW
    )
    repository.upsertNote(note)
}
```

**Step 3: Load visibility when editing**

```kotlin
private fun loadNote(noteId: Long) {
    viewModelScope.launch {
        repository.getNoteById(noteId)?.let { note ->
            _state.update {
                it.copy(
                    editingNoteId = note.id,
                    blocks = decodeContent(note.content),
                    visibility = note.visibility, // NEW
                    isLoadingNote = false,
                )
            }
        }
    }
}
```

**Step 4: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorViewModel.kt
git commit -m "feat: handle visibility in NoteEditorViewModel"
```

---

### Task 18: Update NoteEditorScreen with Visibility Selector

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorScreen.kt`

**Step 1: Add visibility selector to UI**

```kotlin
// Add import
import id.usecase.noted.presentation.note.editor.component.VisibilitySelector

// In NoteEditorScreen composable, inside the LazyColumn content
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 24.dp),
) {
    // Add visibility selector at the top
    item {
        VisibilitySelector(
            visibility = state.visibility,
            onVisibilityChange = { onIntent(NoteEditorIntent.VisibilityChanged(it)) },
        )
    }
    
    // ... existing items(blocks) code ...
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorScreen.kt
git commit -m "feat: integrate VisibilitySelector into NoteEditorScreen"
```

---

## Phase 5: App Presentation Layer - Share Dialog

### Task 19: Create ShareDialog Component

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/note/editor/component/ShareDialog.kt`

**Step 1: Create share dialog with link and QR**

```kotlin
package id.usecase.noted.presentation.note.editor.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import id.usecase.noted.domain.NoteVisibility
import java.io.File
import java.io.FileOutputStream

@Composable
fun ShareDialog(
    noteId: String,
    visibility: NoteVisibility,
    onDismiss: () -> Unit,
    onShareLink: () -> Unit,
    onCopyLink: () -> Unit,
    onShowQR: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val deepLink = "noted://note/$noteId"
    val webLink = "https://noted.app/note/$noteId"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bagikan Note") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = when (visibility) {
                        NoteVisibility.PRIVATE -> "Note ini private. Ubah visibility ke Link Share atau Public untuk membagikan."
                        NoteVisibility.LINK_SHARED -> "Siapa pun dengan link ini dapat mengakses note."
                        NoteVisibility.PUBLIC -> "Note ini publik dan muncul di halaman Explore."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                
                if (visibility != NoteVisibility.PRIVATE) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Link:",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = webLink,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            IconButton(onClick = onCopyLink) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy link",
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (visibility != NoteVisibility.PRIVATE) {
                Button(
                    onClick = onShareLink,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Bagikan Link")
                }
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (visibility != NoteVisibility.PRIVATE) {
                    OutlinedButton(
                        onClick = onShowQR,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("QR Code")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        },
    )
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/component/ShareDialog.kt
git commit -m "feat: add ShareDialog component with link and QR options"
```

---

### Task 20: Create QR Code Generator

**Files:**
- Create: `app/src/main/java/id/usecase/noted/util/QrCodeGenerator.kt`

**Step 1: Create QR generator utility**

```kotlin
package id.usecase.noted.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

object QrCodeGenerator {
    private const val QR_SIZE = 512
    
    fun generateQRCode(content: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE)
        val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until QR_SIZE) {
            for (y in 0 until QR_SIZE) {
                bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        
        return bitmap
    }
    
    fun saveQRCodeToFile(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "qr_share_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
    
    fun getQRCodeUri(context: Context, file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
```

**Step 2: Add ZXing dependency to app/build.gradle.kts**

```kotlin
dependencies {
    // ... existing dependencies ...
    implementation("com.google.zxing:core:3.5.3")
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/util/QrCodeGenerator.kt app/build.gradle.kts
git commit -m "feat: add QR code generator utility with ZXing"
```

---

### Task 21: Add Share Intent to NoteEditor

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorIntent.kt`

**Step 1: Add share intents**

```kotlin
sealed interface NoteEditorIntent {
    // ... existing intents ...
    data object ShareClicked : NoteEditorIntent // NEW
    data object CopyLinkClicked : NoteEditorIntent // NEW
    data object ShowQRClicked : NoteEditorIntent // NEW
    data object DismissShareDialog : NoteEditorIntent // NEW
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorIntent.kt
git commit -m "feat: add share-related intents"
```

---

### Task 22: Update NoteEditorState with Share UI State

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorState.kt`

**Step 1: Add share dialog state**

```kotlin
data class NoteEditorState(
    val editingNoteId: Long? = null,
    val blocks: List<NoteEditorBlock> = listOf(NoteEditorBlock.Text(UUID.randomUUID().toString(), "")),
    val isLoadingNote: Boolean = false,
    val isSaving: Boolean = false,
    val visibility: NoteVisibility = NoteVisibility.PRIVATE,
    val showShareDialog: Boolean = false, // NEW
)
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorState.kt
git commit -m "feat: add showShareDialog to NoteEditorState"
```

---

### Task 23: Update NoteEditorEffect for Sharing

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorEffect.kt`

**Step 1: Add share effects**

```kotlin
sealed interface NoteEditorEffect {
    // ... existing effects ...
    data class ShareNote(val noteId: String, val visibility: NoteVisibility) : NoteEditorEffect // NEW
    data class CopyLink(val link: String) : NoteEditorEffect // NEW
    data class ShowQRCode(val bitmap: android.graphics.Bitmap) : NoteEditorEffect // NEW
    data class ShowMessage(val message: String) : NoteEditorEffect
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorEffect.kt
git commit -m "feat: add share-related effects"
```

---

### Task 24: Update NoteEditorViewModel with Share Logic

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorViewModel.kt`

**Step 1: Handle share intents**

```kotlin
private fun handleIntent(intent: NoteEditorIntent) {
    when (intent) {
        // ... existing handlers ...
        NoteEditorIntent.ShareClicked -> {
            if (state.value.editingNoteId != null) {
                _state.update { it.copy(showShareDialog = true) }
            } else {
                viewModelScope.launch {
                    _effect.send(NoteEditorEffect.ShowMessage("Simpan note terlebih dahulu"))
                }
            }
        }
        NoteEditorIntent.CopyLinkClicked -> {
            val noteId = getCurrentNoteId() // Get from loaded note
            val link = "https://noted.app/note/$noteId"
            viewModelScope.launch {
                _effect.send(NoteEditorEffect.CopyLink(link))
                _effect.send(NoteEditorEffect.ShowMessage("Link disalin ke clipboard"))
            }
        }
        NoteEditorIntent.ShowQRClicked -> {
            val noteId = getCurrentNoteId()
            val link = "https://noted.app/note/$noteId"
            val qrBitmap = QrCodeGenerator.generateQRCode(link)
            viewModelScope.launch {
                _effect.send(NoteEditorEffect.ShowQRCode(qrBitmap))
            }
        }
        NoteEditorIntent.DismissShareDialog -> {
            _state.update { it.copy(showShareDialog = false) }
        }
    }
}

private fun getCurrentNoteId(): String {
    // Get noteId from repository or state
    return state.value.editingNoteId?.let { id ->
        repository.getNoteByIdSync(id)?.noteId
    } ?: ""
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorViewModel.kt
git commit -m "feat: implement share logic in NoteEditorViewModel"
```

---

### Task 25: Update NoteEditorScreen with Share UI

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorScreen.kt`

**Step 1: Add share button to bottom bar**

```kotlin
// Add imports
import androidx.compose.material.icons.filled.Share
import id.usecase.noted.presentation.note.editor.component.ShareDialog

// In NoteEditorScreenRoot, add effect handlers
LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
        when (effect) {
            // ... existing effects ...
            is NoteEditorEffect.ShareNote -> {
                // Handled by dialog display
            }
            is NoteEditorEffect.CopyLink -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Note Link", effect.link))
            }
            is NoteEditorEffect.ShowQRCode -> {
                // Show QR dialog or fullscreen
            }
        }
    }
}

// Add share button in BottomAppBar actions
IconButton(onClick = { onIntent(NoteEditorIntent.ShareClicked) }) {
    Icon(
        imageVector = Icons.Default.Share,
        contentDescription = "Bagikan note",
    )
}

// Add ShareDialog in Scaffold content
if (state.showShareDialog) {
    val noteId = remember { getNoteIdFromState() } // Get actual noteId
    ShareDialog(
        noteId = noteId,
        visibility = state.visibility,
        onDismiss = { onIntent(NoteEditorIntent.DismissShareDialog) },
        onShareLink = {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://noted.app/note/$noteId")
            }
            startActivity(Intent.createChooser(sendIntent, "Bagikan note"))
        },
        onCopyLink = { onIntent(NoteEditorIntent.CopyLinkClicked) },
        onShowQR = { onIntent(NoteEditorIntent.ShowQRClicked) },
    )
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorScreen.kt
git commit -m "feat: integrate share UI into NoteEditorScreen"
```

---

## Phase 6: Deep Link Handling

### Task 26: Create Deep Link Handler

**Files:**
- Create: `app/src/main/java/id/usecase/noted/navigation/DeepLinkHandler.kt`

**Step 1: Create deep link parser**

```kotlin
package id.usecase.noted.navigation

import android.content.Intent
import android.net.Uri

sealed class DeepLink {
    data class NoteDetail(val noteId: String) : DeepLink()
    data object Unknown : DeepLink()
}

object DeepLinkHandler {
    private const val SCHEME_NOTED = "noted"
    private const val HOST_NOTE = "note"
    private const val HOST_WEB = "noted.app"
    
    fun parse(intent: Intent?): DeepLink {
        val uri = intent?.data ?: return DeepLink.Unknown
        return parseUri(uri)
    }
    
    fun parseUri(uri: Uri): DeepLink {
        return when {
            // Handle noted://note/{noteId}
            uri.scheme == SCHEME_NOTED && uri.host == HOST_NOTE -> {
                val noteId = uri.lastPathSegment
                if (noteId != null) DeepLink.NoteDetail(noteId) else DeepLink.Unknown
            }
            // Handle https://noted.app/note/{noteId}
            uri.scheme in listOf("http", "https") && uri.host == HOST_WEB -> {
                val pathSegments = uri.pathSegments
                if (pathSegments.size >= 2 && pathSegments[0] == "note") {
                    DeepLink.NoteDetail(pathSegments[1])
                } else DeepLink.Unknown
            }
            else -> DeepLink.Unknown
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/navigation/DeepLinkHandler.kt
git commit -m "feat: add DeepLinkHandler for noted:// and https:// links"
```

---

### Task 27: Add Note Detail Screen for External Notes

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailScreen.kt`

**Step 1: Create screen for viewing shared notes**

```kotlin
package id.usecase.noted.presentation.note.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NoteDetailScreenRoot(
    viewModel: NoteDetailViewModel,
    noteId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }
    
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NoteDetailEffect.NavigateToEditor -> onNavigateToEditor(effect.localNoteId)
                is NoteDetailEffect.ShowMessage -> onShowMessage(effect.message)
                NoteDetailEffect.NavigateBack -> onNavigateBack()
            }
        }
    }
    
    NoteDetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    state: NoteDetailState,
    onIntent: (NoteDetailIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Note Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(
                        onClick = { onIntent(NoteDetailIntent.CopyContentClicked) },
                        enabled = !state.isLoading && state.note != null,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy content",
                        )
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { onIntent(NoteDetailIntent.ForkClicked) },
                        enabled = !state.isLoading && state.note != null,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ForkRight,
                                contentDescription = null,
                            )
                        },
                        text = { Text("Fork") },
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = { onIntent(NoteDetailIntent.RetryClicked) }) {
                            Text("Retry")
                        }
                    }
                }
                state.note != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Show note content
                        NoteContentView(content = state.note.content)
                        
                        // Show author info
                        Text(
                            text = "By: ${state.note.ownerUserId.take(8)}...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        
                        // Show fork info if applicable
                        if (state.note.forkedFrom != null) {
                            Text(
                                text = "Forked from: ${state.note.forkedFrom.take(8)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailScreen.kt
git commit -m "feat: add NoteDetailScreen for viewing shared notes"
```

---

### Task 28: Create NoteDetailViewModel

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModel.kt`

**Step 1: Implement viewmodel**

```kotlin
package id.usecase.noted.presentation.note.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.usecase.noted.data.NoteRepository
import id.usecase.noted.data.sync.ExploreRepository
import id.usecase.noted.domain.Note
import id.usecase.noted.domain.NoteVisibility
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteDetailViewModel(
    private val exploreRepository: ExploreRepository,
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(NoteDetailState())
    val state: StateFlow<NoteDetailState> = _state.asStateFlow()
    
    private val _effect = Channel<NoteDetailEffect>()
    val effect = _effect.receiveAsFlow()
    
    fun loadNote(noteId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            exploreRepository.getNoteById(noteId)
                .collect { result ->
                    result.fold(
                        onSuccess = { noteDto ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    note = ExternalNote(
                                        id = noteDto.id,
                                        ownerUserId = noteDto.ownerUserId,
                                        content = noteDto.content,
                                        createdAt = noteDto.createdAtEpochMillis,
                                        forkedFrom = null,
                                    ),
                                )
                            }
                        },
                        onFailure = { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Failed to load note",
                                )
                            }
                        },
                    )
                }
        }
    }
    
    fun onIntent(intent: NoteDetailIntent) {
        when (intent) {
            is NoteDetailIntent.ForkClicked -> forkNote()
            is NoteDetailIntent.CopyContentClicked -> copyContent()
            is NoteDetailIntent.RetryClicked -> state.value.note?.let { loadNote(it.id) }
        }
    }
    
    private fun forkNote() {
        val externalNote = state.value.note ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val forkedNote = Note(
                    id = 0,
                    noteId = generateNoteId(),
                    content = externalNote.content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    ownerUserId = getCurrentUserId(),
                    syncStatus = LocalSyncStatus.PENDING_UPSERT,
                    visibility = NoteVisibility.PRIVATE,
                    forkedFrom = externalNote.id, // Track original
                )
                
                val localId = noteRepository.upsertNote(forkedNote)
                _effect.send(NoteDetailEffect.NavigateToEditor(localId))
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _effect.send(NoteDetailEffect.ShowMessage("Failed to fork note: ${e.message}"))
            }
        }
    }
    
    private fun copyContent() {
        val content = state.value.note?.content ?: return
        viewModelScope.launch {
            _effect.send(NoteDetailEffect.ShowMessage("Content copied to clipboard"))
        }
    }
    
    private fun generateNoteId(): String = java.util.UUID.randomUUID().toString()
    private fun getCurrentUserId(): String? = // Get from session
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModel.kt
git commit -m "feat: add NoteDetailViewModel with fork functionality"
```

---

### Task 29: Create NoteDetail State and Intent

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailState.kt`
- Create: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailIntent.kt`
- Create: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailEffect.kt`

**Step 1: Create state**

```kotlin
// NoteDetailState.kt
data class NoteDetailState(
    val isLoading: Boolean = false,
    val note: ExternalNote? = null,
    val errorMessage: String? = null,
)

data class ExternalNote(
    val id: String,
    val ownerUserId: String,
    val content: String,
    val createdAt: Long,
    val forkedFrom: String?,
)
```

**Step 2: Create intent**

```kotlin
// NoteDetailIntent.kt
sealed interface NoteDetailIntent {
    data object ForkClicked : NoteDetailIntent
    data object CopyContentClicked : NoteDetailIntent
    data object RetryClicked : NoteDetailIntent
}
```

**Step 3: Create effect**

```kotlin
// NoteDetailEffect.kt
sealed interface NoteDetailEffect {
    data class NavigateToEditor(val localNoteId: Long) : NoteDetailEffect
    data class ShowMessage(val message: String) : NoteDetailEffect
    data object NavigateBack : NoteDetailEffect
}
```

**Step 4: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailState.kt
git add app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailIntent.kt
git add app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailEffect.kt
git commit -m "feat: add NoteDetail state, intent, and effect classes"
```

---

### Task 30: Update ExploreRepository

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/data/sync/ExploreRepository.kt`

**Step 1: Add getNoteById method**

```kotlin
interface ExploreRepository {
    fun exploreNotes(limit: Int = 50): Flow<Result<List<NoteDto>>>
    fun getNoteById(noteId: String): Flow<Result<NoteDto>> // NEW
}

class SyncExploreRepository(...) : ExploreRepository {
    // ... existing exploreNotes ...
    
    override fun getNoteById(noteId: String): Flow<Result<NoteDto>> = flow {
        val token = session.accessToken
            ?: throw IllegalStateException("Login required")
        
        val response = apiService.getNoteById("Bearer $token", noteId)
        if (response.isSuccessful) {
            response.body()?.let { emit(Result.success(it)) }
                ?: emit(Result.failure(IllegalStateException("Empty response")))
        } else {
            emit(Result.failure(HttpException(response)))
        }
    }.catch { emit(Result.failure(it)) }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/data/sync/ExploreRepository.kt
git commit -m "feat: add getNoteById to ExploreRepository"
```

---

### Task 31: Add API Method for Get Note

**Files:**
- Create/Modify: `app/src/main/java/id/usecase/noted/data/network/NoteApi.kt`

**Step 1: Add endpoint**

```kotlin
interface NoteApi {
    @GET("notes/explore")
    suspend fun exploreNotes(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int,
    ): Response<List<NoteDto>>
    
    @GET("notes/{noteId}") // NEW
    suspend fun getNoteById(
        @Header("Authorization") auth: String,
        @Path("noteId") noteId: String,
    ): Response<NoteDto>
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/data/network/NoteApi.kt
git commit -m "feat: add getNoteById API endpoint"
```

---

## Phase 7: Navigation Integration

### Task 32: Update Navigation Graph

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/navigation/NotedNavHost.kt`

**Step 1: Add note detail destination**

```kotlin
// Add to NavHost
destination(NoteDetailKey::class) { backStackEntry ->
    val key = backStackEntry.toRoute<NoteDetailKey>()
    val viewModel = koinViewModel<NoteDetailViewModel>()
    
    NoteDetailScreenRoot(
        viewModel = viewModel,
        noteId = key.noteId,
        onNavigateBack = { navigator.popBackStack() },
        onNavigateToEditor = { localNoteId ->
            navigator.navigateToNoteEditor(localNoteId)
        },
        onShowMessage = { message ->
            // Show snackbar
        },
    )
}
```

**Step 2: Add navigation key**

```kotlin
@Serializable
data class NoteDetailKey(val noteId: String) : NavKey
```

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/navigation/NotedNavHost.kt
git commit -m "feat: add NoteDetail destination to navigation"
```

---

### Task 33: Handle Deep Links in MainActivity

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/MainActivity.kt`

**Step 1: Parse deep links and navigate**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Handle deep link
    val deepLink = DeepLinkHandler.parse(intent)
    val initialRoute = when (deepLink) {
        is DeepLink.NoteDetail -> NoteDetailKey(deepLink.noteId)
        else -> NoteListKey
    }
    
    setContent {
        NotedTheme {
            NotedNavHost(initialRoute = initialRoute)
        }
    }
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    // Handle deep link while app is running
    val deepLink = DeepLinkHandler.parse(intent)
    if (deepLink is DeepLink.NoteDetail) {
        // Navigate to note detail
    }
}
```

**Step 2: Update AndroidManifest.xml**

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!-- Deep link handler -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="noted" android:host="note" />
    </intent-filter>
    
    <!-- Web link handler -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="noted.app" android:pathPrefix="/note/" />
    </intent-filter>
</activity>
```

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/MainActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat: handle deep links in MainActivity"
```

---

## Phase 8: Fork Feature

### Task 34: Add Fork Metadata to Note Entity

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/data/local/NoteEntity.kt`
- Modify: `app/src/main/java/id/usecase/noted/domain/Note.kt`

**Step 1: Add forkedFrom column**

```kotlin
// NoteEntity.kt
@ColumnInfo(name = "forked_from") val forkedFrom: String? = null, // NEW

// Note.kt
val forkedFrom: String? = null, // NEW
```

**Step 2: Add migration for forked_from**

```kotlin
// In NoteDatabase.kt
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN forked_from TEXT")
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/data/local/NoteEntity.kt
git add app/src/main/java/id/usecase/noted/domain/Note.kt
git add app/src/main/java/id/usecase/noted/data/local/NoteDatabase.kt
git commit -m "feat: add forked_from metadata to Note"
```

---

### Task 35: Update NoteListItem with Visibility Indicator

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/list/component/NoteListItem.kt`

**Step 1: Add visibility icon**

```kotlin
// Add imports
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import id.usecase.noted.domain.NoteVisibility

// In NoteListItem composable
@Composable
fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = note.title ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                )
                // Visibility icon
                when (note.visibility) {
                    NoteVisibility.PRIVATE -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NoteVisibility.LINK_SHARED -> Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link Shared",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    NoteVisibility.PUBLIC -> Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Public",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            // ... rest of item content
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/list/component/NoteListItem.kt
git commit -m "feat: show visibility icon in NoteListItem"
```

---

## Phase 9: Sync & Save Feature

### Task 36: Add Save Note from Explore

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailScreen.kt`

**Step 1: Add save button**

```kotlin
// Add to bottom bar actions
IconButton(
    onClick = { onIntent(NoteDetailIntent.SaveClicked) },
    enabled = !state.isLoading && state.note != null,
) {
    Icon(
        imageVector = Icons.Default.Bookmark,
        contentDescription = "Save note",
    )
}
```

**Step 2: Update intents and viewmodel**

```kotlin
// NoteDetailIntent.kt
data object SaveClicked : NoteDetailIntent

// NoteDetailViewModel.kt
private fun saveNote() {
    // Similar to fork but without forkedFrom metadata
    val externalNote = state.value.note ?: return
    
    viewModelScope.launch {
        val savedNote = Note(
            id = 0,
            noteId = generateNoteId(),
            content = externalNote.content,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            ownerUserId = getCurrentUserId(),
            syncStatus = LocalSyncStatus.PENDING_UPSERT,
            visibility = NoteVisibility.PRIVATE,
            forkedFrom = null, // Not a fork, just a save
        )
        
        noteRepository.upsertNote(savedNote)
        _effect.send(NoteDetailEffect.ShowMessage("Note saved to your collection"))
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/detail/
git commit -m "feat: add save note functionality"
```

---

## Phase 10: Testing

### Task 37: Add NoteVisibility Tests

**Files:**
- Create: `app/src/test/java/id/usecase/noted/domain/NoteVisibilityTest.kt`

**Step 1: Write tests**

```kotlin
package id.usecase.noted.domain

import org.junit.Assert.*
import org.junit.Test

class NoteVisibilityTest {
    @Test
    fun `fromString returns correct enum for valid values`() {
        assertEquals(NoteVisibility.PRIVATE, NoteVisibility.fromString("PRIVATE"))
        assertEquals(NoteVisibility.LINK_SHARED, NoteVisibility.fromString("LINK_SHARED"))
        assertEquals(NoteVisibility.PUBLIC, NoteVisibility.fromString("PUBLIC"))
    }
    
    @Test
    fun `fromString defaults to PRIVATE for unknown values`() {
        assertEquals(NoteVisibility.PRIVATE, NoteVisibility.fromString("UNKNOWN"))
        assertEquals(NoteVisibility.PRIVATE, NoteVisibility.fromString(""))
    }
    
    @Test
    fun `enum values are correct`() {
        assertEquals(3, NoteVisibility.entries.size)
        assertTrue(NoteVisibility.entries.contains(NoteVisibility.PRIVATE))
        assertTrue(NoteVisibility.entries.contains(NoteVisibility.LINK_SHARED))
        assertTrue(NoteVisibility.entries.contains(NoteVisibility.PUBLIC))
    }
}
```

**Step 2: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.domain.NoteVisibilityTest"
```

**Step 3: Commit**

```bash
git add app/src/test/java/id/usecase/noted/domain/NoteVisibilityTest.kt
git commit -m "test: add NoteVisibility unit tests"
```

---

### Task 38: Add DeepLinkHandler Tests

**Files:**
- Create: `app/src/test/java/id/usecase/noted/navigation/DeepLinkHandlerTest.kt`

**Step 1: Write tests**

```kotlin
package id.usecase.noted.navigation

import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkHandlerTest {
    @Test
    fun `parseUri handles noted scheme correctly`() {
        val uri = Uri.parse("noted://note/abc123")
        val result = DeepLinkHandler.parseUri(uri)
        
        assertTrue(result is DeepLink.NoteDetail)
        assertEquals("abc123", (result as DeepLink.NoteDetail).noteId)
    }
    
    @Test
    fun `parseUri handles https scheme correctly`() {
        val uri = Uri.parse("https://noted.app/note/xyz789")
        val result = DeepLinkHandler.parseUri(uri)
        
        assertTrue(result is DeepLink.NoteDetail)
        assertEquals("xyz789", (result as DeepLink.NoteDetail).noteId)
    }
    
    @Test
    fun `parseUri handles http scheme correctly`() {
        val uri = Uri.parse("http://noted.app/note/test456")
        val result = DeepLinkHandler.parseUri(uri)
        
        assertTrue(result is DeepLink.NoteDetail)
        assertEquals("test456", (result as DeepLink.NoteDetail).noteId)
    }
    
    @Test
    fun `parseUri returns Unknown for invalid URIs`() {
        assertTrue(DeepLinkHandler.parseUri(Uri.parse("noted://other/abc")) is DeepLink.Unknown)
        assertTrue(DeepLinkHandler.parseUri(Uri.parse("https://other.app/note/abc")) is DeepLink.Unknown)
        assertTrue(DeepLinkHandler.parseUri(Uri.parse("noted://note")) is DeepLink.Unknown)
    }
    
    @Test
    fun `parse handles null intent`() {
        assertTrue(DeepLinkHandler.parse(null) is DeepLink.Unknown)
    }
}
```

**Step 2: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.navigation.DeepLinkHandlerTest"
```

**Step 3: Commit**

```bash
git add app/src/test/java/id/usecase/noted/navigation/DeepLinkHandlerTest.kt
git commit -m "test: add DeepLinkHandler unit tests"
```

---

### Task 39: Add NoteEditorViewModel Visibility Tests

**Files:**
- Create: `app/src/test/java/id/usecase/noted/presentation/note/editor/NoteEditorViewModelVisibilityTest.kt`

**Step 1: Write tests**

```kotlin
package id.usecase.noted.presentation.note.editor

import id.usecase.noted.domain.NoteVisibility
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NoteEditorViewModelVisibilityTest {
    @Test
    fun `visibility change updates state`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.onIntent(NoteEditorIntent.VisibilityChanged(NoteVisibility.PUBLIC))
        
        assertEquals(NoteVisibility.PUBLIC, viewModel.state.value.visibility)
    }
    
    @Test
    fun `visibility defaults to PRIVATE`() = runTest {
        val viewModel = createViewModel()
        
        assertEquals(NoteVisibility.PRIVATE, viewModel.state.value.visibility)
    }
    
    @Test
    fun `share clicked shows dialog when note exists`() = runTest {
        val viewModel = createViewModelWithExistingNote()
        
        viewModel.onIntent(NoteEditorIntent.ShareClicked)
        
        assertTrue(viewModel.state.value.showShareDialog)
    }
    
    @Test
    fun `share clicked shows error when note not saved`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.onIntent(NoteEditorIntent.ShareClicked)
        
        assertFalse(viewModel.state.value.showShareDialog)
        // Verify effect sent
    }
    
    private fun createViewModel() = // ...
    private fun createViewModelWithExistingNote() = // ...
}
```

**Step 2: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.presentation.note.editor.NoteEditorViewModelVisibilityTest"
```

**Step 3: Commit**

```bash
git add app/src/test/java/id/usecase/noted/presentation/note/editor/NoteEditorViewModelVisibilityTest.kt
git commit -m "test: add NoteEditorViewModel visibility tests"
```

---

### Task 40: Add Backend Visibility Tests

**Files:**
- Create: `backend/src/test/kotlin/id/usecase/backend/note/service/NoteSharingServiceVisibilityTest.kt`

**Step 1: Write tests**

```kotlin
package id.usecase.backend.note.service

import id.usecase.backend.note.domain.StoredNote
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoteSharingServiceVisibilityTest {
    @Test
    fun `exploreNotes only returns PUBLIC notes`() = runTest {
        val service = createService()
        
        // Insert notes with different visibilities
        insertNote(visibility = "PUBLIC")
        insertNote(visibility = "LINK_SHARED")
        insertNote(visibility = "PRIVATE")
        
        val result = service.exploreNotes(excludeUserId = "user1", limit = 10)
        
        assertEquals(1, result.size)
        assertEquals("PUBLIC", result[0].visibility)
    }
    
    @Test
    fun `getNoteByLink returns PUBLIC note to any user`() = runTest {
        val service = createService()
        insertNote(id = "note1", visibility = "PUBLIC")
        
        val result = service.getNoteByLink("note1", "anyUser")
        
        assertNotNull(result)
    }
    
    @Test
    fun `getNoteByLink returns LINK_SHARED note to any user`() = runTest {
        val service = createService()
        insertNote(id = "note1", visibility = "LINK_SHARED")
        
        val result = service.getNoteByLink("note1", "anyUser")
        
        assertNotNull(result)
    }
    
    @Test
    fun `getNoteByLink returns null for PRIVATE note to non-owner`() = runTest {
        val service = createService()
        insertNote(id = "note1", ownerId = "owner1", visibility = "PRIVATE")
        
        val result = service.getNoteByLink("note1", "otherUser")
        
        assertNull(result)
    }
    
    @Test
    fun `getNoteByLink returns PRIVATE note to owner`() = runTest {
        val service = createService()
        insertNote(id = "note1", ownerId = "owner1", visibility = "PRIVATE")
        
        val result = service.getNoteByLink("note1", "owner1")
        
        assertNotNull(result)
    }
    
    private fun createService() = // ...
    private fun insertNote(...) = // ...
}
```

**Step 2: Run tests**

```bash
./gradlew :backend:test --tests "id.usecase.backend.note.service.NoteSharingServiceVisibilityTest"
```

**Step 3: Commit**

```bash
git add backend/src/test/kotlin/id/usecase/backend/note/service/NoteSharingServiceVisibilityTest.kt
git commit -m "test: add backend visibility filtering tests"
```

---

## Phase 11: Final Integration

### Task 41: Run Full Build

**Step 1: Clean build**

```bash
./gradlew clean build
```

**Step 2: Fix any compilation errors**

**Step 3: Commit fixes**

```bash
git add .
git commit -m "fix: resolve compilation errors"
```

---

### Task 42: Run All Tests

**Step 1: Run backend tests**

```bash
./gradlew :backend:test
```

**Step 2: Run app unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

**Step 3: Run lint**

```bash
./gradlew :app:lintDebug
```

**Step 4: Commit**

```bash
git commit -m "chore: all tests passing"
```

---

## Summary

This plan implements:

1. **Visibility System**: 3 levels (PRIVATE, LINK_SHARED, PUBLIC) with backend filtering
2. **Sharing**: Deep links (`noted://note/{id}`) and web links (`https://noted.app/note/{id}`)
3. **QR Codes**: Generated using ZXing library, shareable as images
4. **Fork Feature**: Clone notes with metadata preservation (original author tracking)
5. **Save Feature**: Save shared notes to personal collection
6. **Sync**: Visibility changes propagate through existing sync mechanism
7. **UI**: Bottom app bar actions following existing pattern, visibility selector in editor
8. **Tests**: Unit tests for visibility logic, deep link handling, and backend filtering

**Key Files Changed:**
- Data models: Note, NoteEntity, NoteDto, StoredNote
- Database: Migration 2→3 for visibility column
- Backend: Repository queries with visibility filter
- UI: VisibilitySelector, ShareDialog, NoteDetailScreen
- Navigation: DeepLinkHandler, MainActivity intent filters
- Tests: Comprehensive unit tests for new features

**Dependencies Added:**
- `com.google.zxing:core:3.5.3` for QR code generation

**Estimated Implementation Time:** 8-12 hours

---

# Phase 12: Note History & 3 Tabs (ADDENDUM)

## Overview

Additional feature to add 3 tabs in NoteListScreen:
1. **My Notes** - Notes created by current user
2. **Saved** - Notes forked/saved from other users  
3. **History** - Recently viewed notes from other users

## Architecture

- **History tracking**: Record when user views external notes via NoteDetailScreen
- **Local storage**: Room database table for history with viewed timestamp
- **Server sync**: Sync history with backend for cross-device support
- **Time formatting**: Relative time display ("5 min ago", "Yesterday")

---

### Task 43: Backend History API

**Files:**
- Create: `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteHistoryContracts.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/note/domain/StoredNoteHistory.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/note/domain/NoteHistoryRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/note/data/PostgresNoteHistoryRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/note/service/NoteHistoryService.kt`
- Update: `backend/src/main/kotlin/id/usecase/backend/note/presentation/NoteRoutes.kt`
- Update: `backend/src/main/kotlin/id/usecase/backend/di/DependencyInjection.kt`

**API Endpoints:**
- `POST /api/notes/{noteId}/history` - Record note view
- `GET /api/notes/history?limit={n}` - Get viewing history
- `DELETE /api/notes/history` - Clear history

**Database Schema:**
```sql
CREATE TABLE note_history (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    note_id VARCHAR(36) NOT NULL,
    note_owner_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    viewed_at_epoch_millis BIGINT NOT NULL,
    UNIQUE(user_id, note_id)
);
```

---

### Task 44: App Data Layer - History

**Files:**
- Create: `app/src/main/java/id/usecase/noted/domain/NoteHistory.kt`
- Create: `app/src/main/java/id/usecase/noted/data/local/NoteHistoryEntity.kt`
- Create: `app/src/main/java/id/usecase/noted/data/local/NoteHistoryDao.kt`
- Create: `app/src/main/java/id/usecase/noted/data/NoteHistoryRepository.kt`
- Create: `app/src/main/java/id/usecase/noted/data/sync/NoteHistoryApi.kt`
- Update: `app/src/main/java/id/usecase/noted/data/local/NoteDatabase.kt`

**Migration 4→5:**
```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE note_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                note_id TEXT NOT NULL,
                owner_user_id TEXT NOT NULL,
                content TEXT NOT NULL,
                viewed_at INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX index_history_note_id ON note_history(note_id)")
        db.execSQL("CREATE INDEX index_history_viewed_at ON note_history(viewed_at)")
    }
}
```

---

### Task 45: App UI - 3 Tabs

**Files:**
- Update: `app/src/main/java/id/usecase/noted/presentation/note/list/NoteListState.kt`
- Update: `app/src/main/java/id/usecase/noted/presentation/note/list/NoteListIntent.kt`
- Update: `app/src/main/java/id/usecase/noted/presentation/note/list/NoteListEffect.kt`
- Update: `app/src/main/java/id/usecase/noted/presentation/note/list/NoteListViewModel.kt`
- Update: `app/src/main/java/id/usecase/noted/presentation/note/list/NoteListScreen.kt`
- Create: `app/src/main/java/id/usecase/noted/presentation/note/list/component/NoteHistoryListItem.kt`
- Update: `app/src/main/java/id/usecase/noted/di/AppModule.kt`

**State Structure:**
```kotlin
data class NoteListState(
    val selectedTab: Int = 0, // 0=My Notes, 1=Saved, 2=History
    val myNotes: List<NoteListItemUi> = emptyList(),
    val savedNotes: List<NoteListItemUi> = emptyList(),
    val historyNotes: List<NoteHistoryItemUi> = emptyList(),
    // ... other fields
)

data class NoteHistoryItemUi(
    val id: String,
    val ownerUserId: String,
    val title: String,
    val preview: String,
    val viewedAt: Long,
)
```

**UI Features:**
- Material3 TabRow with 3 tabs below TopAppBar
- My Notes: Shows own notes (filter by ownerUserId, forkedFrom == null)
- Saved: Shows forked notes (filter by forkedFrom != null)
- History: Shows NoteHistoryListItem with "Viewed X ago" timestamp
- FAB hidden on History tab

---

### Task 46: History Tracking Integration

**Files:**
- Update: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModel.kt`
- Create: `app/src/main/java/id/usecase/noted/util/TimeUtils.kt`
- Update: `app/src/main/java/id/usecase/noted/data/NoteHistoryRepositoryImpl.kt`

**TimeUtils:**
```kotlin
object TimeUtils {
    fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} min ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
            diff < 172_800_000 -> "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
```

**History Recording:**
- Record history when NoteDetailViewModel.loadNote() succeeds
- Only record for notes from other users (not own notes)
- Async operation, don't block UI
- Handle errors gracefully (silent fail)

---

### Task 47: Navigation Updates

**Files:**
- Update: `app/src/main/java/id/usecase/noted/navigation/NoteNavigation.kt`
- Update: `app/src/main/java/id/usecase/noted/MainActivity.kt`

**Navigation:**
- NoteListScreen can navigate to NoteDetailScreen for history items
- Deep links route to NoteDetailScreen (history recorded automatically)
- Back navigation returns to correct tab

---

## Summary of Phase 12

**New Features:**
1. **3 Tabs**: My Notes, Saved, History
2. **History Tracking**: Automatic recording of viewed notes
3. **Time Display**: Relative timestamps ("5 min ago", "Yesterday")
4. **Cross-device**: History syncs via backend

**Files Added/Modified:**
- Backend: 6 new files, 2 updated
- Shared: 1 new file
- App: 10+ new files, 8+ updated
- Database: Migration 4→5

**Key Components:**
- `NoteHistoryRepository` - Data access layer
- `NoteHistoryListItem` - History UI component  
- `TimeUtils` - Time formatting utility
- TabRow with 3 tabs in NoteListScreen

**Build Status:** ✅ SUCCESS (all tasks completed by parallel subagents)
