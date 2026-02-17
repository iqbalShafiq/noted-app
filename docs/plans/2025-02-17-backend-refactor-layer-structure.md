# Backend Refactor & Shared Module Enhancement - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.
> **Required Skills:** @superpowers/dispatching-parallel-agents, @superpowers/using-git-worktrees

**Goal:** Refactor backend dari feature-first ke layer-first structure dan kaya-kan shared module dengan domain models, menghilangkan duplikasi antar app & backend.

**Architecture:** 
- Backend: Flat layer-first structure (domain/, data/, service/, presentation/)
- Shared: Rich domain models dengan kotlinx.serialization untuk cross-platform consistency
- App: Hanya Android-specific implementations tetap (Room entities, DataStore, etc.)

**Tech Stack:** Kotlin 17, Ktor, kotlinx.serialization, Koin DI, Room, PostgreSQL

---

## Phase 0: Preparation & Setup

### Task 0.1: Create Git Worktree for Safe Refactoring

**Files:**
- Worktree: `../noted-refactor-worktree`

**Step 1: Create worktree from current branch**

```bash
cd /Users/shafiq/AndroidStudioProjects/Noted
git worktree add ../noted-refactor-worktree -b refactor/backend-layer-structure
cd ../noted-refactor-worktree
```

**Step 2: Verify build passes before changes**

```bash
./gradlew :shared:build :backend:build :app:assembleDebug
```

Expected: All modules build successfully

**Step 3: Commit marker**

```bash
git add .
git commit -m "chore: create worktree for backend refactor"
```

---

## Phase 1: Enhance Shared Module with Domain Models

### Task 1.1: Add NoteVisibility Enum to Shared Module

**Files:**
- Create: `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteVisibility.kt`
- Delete: `app/src/main/java/id/usecase/noted/domain/NoteVisibility.kt`
- Delete: `backend/src/main/kotlin/id/usecase/backend/note/domain/NoteRepository.kt` (lines 5-9 only)

**Step 1: Create shared NoteVisibility enum**

```kotlin
package id.usecase.noted.shared.note

import kotlinx.serialization.Serializable

@Serializable
enum class NoteVisibility {
    PRIVATE,
    LINK_SHARED,
    PUBLIC,
}
```

**Step 2: Run shared build**

```bash
./gradlew :shared:build
```

Expected: Build passes

**Step 3: Commit**

```bash
git add shared/
git commit -m "feat(shared): add NoteVisibility enum"
```

---

### Task 1.2: Create Shared Note Domain Model

**Files:**
- Create: `shared/src/main/kotlin/id/usecase/noted/shared/note/Note.kt`

**Step 1: Create shared Note data class**

```kotlin
package id.usecase.noted.shared.note

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val noteId: String,
    val ownerUserId: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val version: Long = 1,
    val visibility: NoteVisibility = NoteVisibility.PRIVATE,
    val forkedFrom: String? = null,
    val sharedWithUserIds: List<String> = emptyList(),
)
```

**Step 2: Build shared module**

```bash
./gradlew :shared:build
```

**Step 3: Commit**

```bash
git add shared/
git commit -m "feat(shared): add Note domain model"
```

---

### Task 1.3: Create Shared User Domain Model

**Files:**
- Create: `shared/src/main/kotlin/id/usecase/noted/shared/user/User.kt`

**Step 1: Create shared User data class**

```kotlin
package id.usecase.noted.shared.user

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String,
    val username: String,
    val displayName: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null,
    val email: String? = null,
    val createdAtEpochMillis: Long,
    val lastLoginAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long = 0,
)

@Serializable
data class UserStatistics(
    val totalNotes: Int = 0,
    val notesShared: Int = 0,
    val notesReceived: Int = 0,
    val lastSyncAtEpochMillis: Long? = null,
)
```

**Step 2: Build and commit**

```bash
./gradlew :shared:build
```

Expected: Build passes

```bash
git add shared/
git commit -m "feat(shared): add User and UserStatistics domain models"
```

---

### Task 1.4: Create Shared NoteHistory Domain Model

**Files:**
- Create: `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteHistory.kt`

**Step 1: Create shared NoteHistory data class**

```kotlin
package id.usecase.noted.shared.note

import kotlinx.serialization.Serializable

@Serializable
data class NoteHistory(
    val noteId: String,
    val ownerUserId: String,
    val content: String,
    val viewedAtEpochMillis: Long,
)
```

**Step 2: Build and commit**

```bash
./gradlew :shared:build
```

```bash
git add shared/
git commit -m "feat(shared): add NoteHistory domain model"
```

---

### Task 1.5: Create Shared Domain Error Types

**Files:**
- Create: `shared/src/main/kotlin/id/usecase/noted/shared/error/DomainError.kt`

**Step 1: Create shared error types**

```kotlin
package id.usecase.noted.shared.error

import kotlinx.serialization.Serializable

@Serializable
sealed class DomainError {
    abstract val message: String

    @Serializable
    data class ValidationError(override val message: String) : DomainError()

    @Serializable
    data class NotFoundError(override val message: String) : DomainError()

    @Serializable
    data class ConflictError(override val message: String) : DomainError()

    @Serializable
    data class UnauthorizedError(override val message: String) : DomainError()

    @Serializable
    data class NetworkError(override val message: String) : DomainError()

    @Serializable
    data class UnknownError(override val message: String) : DomainError()
}
```

**Step 2: Build and commit**

```bash
./gradlew :shared:build
```

```bash
git add shared/
git commit -m "feat(shared): add DomainError sealed class"
```

---

## Phase 2: Refactor Backend to Layer-First Structure

### Task 2.1: Create Backend Domain Layer Structure

**Files:**
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/note/NoteRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/note/NoteSyncRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/note/NoteHistoryRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/note/NoteShareRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/auth/AuthRepository.kt`

**Step 1: Create NoteRepository interface**

```kotlin
package id.usecase.backend.domain.note

import id.usecase.noted.shared.note.Note

interface NoteRepository {
    suspend fun create(note: Note): Note
    suspend fun findById(noteId: String): Note?
    suspend fun findByOwner(ownerUserId: String): List<Note>
    suspend fun findByIds(noteIds: Set<String>): List<Note>
    suspend fun findAllExcludingOwner(excludeOwnerUserId: String, limit: Int = 50): List<Note>
    suspend fun findPublicNotes(limit: Int = 50): List<Note>
    suspend fun searchPublicNotes(query: String, excludeOwnerUserId: String, limit: Int = 50): List<Note>
    suspend fun update(note: Note): Note
    suspend fun delete(noteId: String): Boolean
}
```

**Step 2: Create NoteSyncRepository interface**

```kotlin
package id.usecase.backend.domain.note

import id.usecase.noted.shared.note.SyncMutationDto

interface NoteSyncRepository {
    suspend fun applyMutation(
        ownerUserId: String,
        mutation: SyncMutationDto,
        serverNowEpochMillis: Long,
    ): SyncApplyResult

    suspend fun pullChanges(ownerUserId: String, afterCursor: Long): SyncPullData
    suspend fun currentCursor(ownerUserId: String): Long
}

data class SyncPullData(
    val cursor: Long,
    val notes: List<Note>,
)

data class SyncApplyResult(
    val status: SyncApplyStatus,
    val appliedNote: Note? = null,
    val conflictReason: String? = null,
    val conflictServerNote: Note? = null,
)

enum class SyncApplyStatus {
    APPLIED,
    DUPLICATE,
    CONFLICT,
}
```

**Step 3: Create NoteHistoryRepository interface**

```kotlin
package id.usecase.backend.domain.note

import id.usecase.noted.shared.note.NoteHistory

interface NoteHistoryRepository {
    suspend fun record(history: NoteHistory): NoteHistory
    suspend fun findByUser(userId: String, limit: Int = 50): List<NoteHistory>
    suspend fun clearHistory(userId: String): Boolean
}
```

**Step 4: Create NoteShareRepository interface**

```kotlin
package id.usecase.backend.domain.note

import id.usecase.noted.shared.note.Note

data class NoteShare(
    val noteId: String,
    val recipientUserId: String,
    val sharedAtEpochMillis: Long,
)

interface NoteShareRepository {
    suspend fun share(noteId: String, recipientUserId: String): NoteShare
    suspend fun findSharesByNote(noteId: String): List<NoteShare>
    suspend fun findSharesByRecipient(recipientUserId: String): List<NoteShare>
}
```

**Step 5: Create AuthRepository interface**

```kotlin
package id.usecase.backend.domain.auth

import id.usecase.noted.shared.user.User
import id.usecase.noted.shared.user.UserStatistics

interface AuthRepository {
    suspend fun createUser(username: String, passwordHash: String, createdAtEpochMillis: Long): User
    suspend fun findByUsername(username: String): User?
    suspend fun findById(userId: String): User?
    suspend fun updatePasswordHashByUsername(username: String, passwordHash: String): User
    suspend fun updateProfile(
        userId: String,
        displayName: String?,
        bio: String?,
        profilePictureUrl: String?,
        email: String?,
    ): User
    suspend fun updateLastLogin(userId: String): User
    suspend fun getUserStatistics(userId: String): UserStatistics
}
```

**Step 6: Delete old domain files**

```bash
rm -rf backend/src/main/kotlin/id/usecase/backend/note/domain/
rm -rf backend/src/main/kotlin/id/usecase/backend/auth/domain/
```

**Step 7: Build backend**

```bash
./gradlew :backend:build
```

Expected: Compilation errors expected (implementations not updated yet)

**Step 8: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/domain/
git commit -m "feat(backend): create layer-first domain structure with shared models"
```

---

### Task 2.2: Move Data Implementations to New Structure

**Files:**
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteRepository.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteRepository.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteHistoryRepository.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteHistoryRepository.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteShareRepository.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteShareRepository.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/data/auth/InMemoryAuthRepository.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/data/auth/PostgresAuthRepository.kt`

**Step 1: Update InMemoryNoteRepository package and imports**

Change from:
```kotlin
package id.usecase.backend.note.data
import id.usecase.backend.note.domain.NoteRepository
import id.usecase.backend.note.domain.NoteSyncRepository
import id.usecase.backend.note.domain.StoredNote
```

To:
```kotlin
package id.usecase.backend.data.note
import id.usecase.backend.domain.note.NoteRepository
import id.usecase.backend.domain.note.NoteSyncRepository
import id.usecase.backend.domain.note.SyncApplyResult
import id.usecase.backend.domain.note.SyncApplyStatus
import id.usecase.backend.domain.note.SyncPullData
import id.usecase.noted.shared.note.Note
import id.usecase.noted.shared.note.NoteVisibility
```

And update all `StoredNote` references to `Note`.

**Step 2: Update PostgresNoteRepository similarly**

Update package declaration and imports. Update SQL mappings from `StoredNote` to `Note`.

**Step 3: Update NoteHistory repositories**

Change imports from old domain to new domain, update `StoredNoteHistory` to `NoteHistory`.

**Step 4: Update NoteShare repositories**

Change imports and update `StoredNoteShare` to `NoteShare`.

**Step 5: Update Auth repositories**

Change imports from `AuthUser` to `User` (from shared module).

**Step 6: Delete old data directories**

```bash
rm -rf backend/src/main/kotlin/id/usecase/backend/note/data/
rm -rf backend/src/main/kotlin/id/usecase/backend/auth/data/
```

**Step 7: Build to check errors**

```bash
./gradlew :backend:compileKotlin 2>&1 | head -50
```

Fix any remaining import errors.

**Step 8: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/data/
git commit -m "refactor(backend): move data implementations to layer-first structure"
```

---

### Task 2.3: Move Services to New Structure

**Files:**
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/service/note/NoteSharingService.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/service/note/NoteHistoryService.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/service/auth/AuthService.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/service/sync/NoteSyncService.kt`

**Step 1: Update NoteSharingService package and imports**

```kotlin
package id.usecase.backend.service.note

import id.usecase.backend.domain.note.NoteRepository
import id.usecase.backend.domain.note.NoteShareRepository
import id.usecase.backend.domain.note.NoteShare
import id.usecase.noted.shared.note.Note
import id.usecase.noted.shared.note.NoteVisibility
// ... rest of imports
```

**Step 2: Update other services similarly**

Update all references from old domain paths to new domain paths.

**Step 3: Delete old service directories**

```bash
rm -rf backend/src/main/kotlin/id/usecase/backend/note/service/
rm -rf backend/src/main/kotlin/id/usecase/backend/auth/service/
rm -rf backend/src/main/kotlin/id/usecase/backend/sync/service/
```

**Step 4: Build**

```bash
./gradlew :backend:compileKotlin
```

**Step 5: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/service/
git commit -m "refactor(backend): move services to layer-first structure"
```

---

### Task 2.4: Move Presentation/Routes to New Structure

**Files:**
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/presentation/note/NoteRoutes.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/presentation/auth/AuthRoutes.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/presentation/sync/SyncRoutes.kt`
- Move & Modify: `backend/src/main/kotlin/id/usecase/backend/presentation/user/UserRoutes.kt`

**Step 1: Update NoteRoutes package and imports**

```kotlin
package id.usecase.backend.presentation.note

import id.usecase.backend.service.note.NoteSharingService
import id.usecase.backend.service.note.NoteHistoryService
// ... other imports
```

**Step 2: Update other route files**

Update all imports to use new service locations.

**Step 3: Update DI configuration**

Modify: `backend/src/main/kotlin/id/usecase/backend/di/DependencyInjection.kt`

Update all Koin module declarations:
- `id.usecase.backend.note.data.*` → `id.usecase.backend.data.note.*`
- `id.usecase.backend.note.domain.*` → `id.usecase.backend.domain.note.*`
- `id.usecase.backend.note.service.*` → `id.usecase.backend.service.note.*`

**Step 4: Update Routing configuration**

Modify: `backend/src/main/kotlin/id/usecase/backend/plugins/Routing.kt`

Update imports to use new presentation locations:
- `id.usecase.backend.note.presentation.*` → `id.usecase.backend.presentation.note.*`
- `id.usecase.backend.auth.presentation.*` → `id.usecase.backend.presentation.auth.*`
- `id.usecase.backend.sync.presentation.*` → `id.usecase.backend.presentation.sync.*`

**Step 5: Delete old presentation directories**

```bash
rm -rf backend/src/main/kotlin/id/usecase/backend/note/presentation/
rm -rf backend/src/main/kotlin/id/usecase/backend/auth/presentation/
rm -rf backend/src/main/kotlin/id/usecase/backend/sync/presentation/
rm -rf backend/src/main/kotlin/id/usecase/backend/user/
```

**Step 6: Build and test**

```bash
./gradlew :backend:build
./gradlew :backend:test
```

Expected: All tests pass

**Step 7: Commit**

```bash
git add backend/
git commit -m "refactor(backend): move presentation routes to layer-first structure"
```

---

## Phase 3: Update Android App to Use Shared Domain Models

### Task 3.1: Update App Domain Models to Use Shared

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/domain/Note.kt`
- Delete: `app/src/main/java/id/usecase/noted/domain/NoteVisibility.kt`
- Delete: `app/src/main/java/id/usecase/noted/domain/NoteHistory.kt`

**Step 1: Refactor app's Note domain model**

Current `Note.kt` imports `LocalSyncStatus` which creates circular dependency. Solution: Remove syncStatus from domain Note, keep it in entity only.

Modify `app/src/main/java/id/usecase/noted/domain/Note.kt`:

```kotlin
package id.usecase.noted.domain

import id.usecase.noted.shared.note.Note as SharedNote
import id.usecase.noted.shared.note.NoteVisibility

// Re-export from shared module for convenience
typealias Note = SharedNote
typealias NoteVisibility = id.usecase.noted.shared.note.NoteVisibility

// App-specific extension properties
val Note.localId: Long
    get() = TODO("Map from local database")

// OR: If we need local ID, create wrapper
data class LocalNote(
    val localId: Long,
    val note: Note,
    val syncStatus: LocalSyncStatus,
)
```

Actually, better approach: Keep using SharedNote directly in most places. For Room, use NoteEntity separately.

**Step 2: Delete old domain files**

```bash
rm app/src/main/java/id/usecase/noted/domain/NoteVisibility.kt
rm app/src/main/java/id/usecase/noted/domain/NoteHistory.kt
```

**Step 3: Update imports in app**

Files to update:
- `app/src/main/java/id/usecase/noted/data/NoteRepository.kt`
- `app/src/main/java/id/usecase/noted/data/RoomNoteRepository.kt`
- All ViewModels that use Note
- All tests

Replace:
- `id.usecase.noted.domain.Note` → `id.usecase.noted.shared.note.Note`
- `id.usecase.noted.domain.NoteVisibility` → `id.usecase.noted.shared.note.NoteVisibility`
- `id.usecase.noted.domain.NoteHistory` → `id.usecase.noted.shared.note.NoteHistory`

**Step 4: Update RoomNoteRepository to map between NoteEntity and shared Note**

The repository will work with `id.usecase.noted.shared.note.Note` in its public interface but internally map to/from `NoteEntity`.

**Step 5: Build app**

```bash
./gradlew :app:compileDebugKotlin
```

Fix any compilation errors.

**Step 6: Commit**

```bash
git add app/
git commit -m "refactor(app): migrate to shared domain models"
```

---

### Task 3.2: Update All Import Statements in App

**Files:**
- Modify: All files importing old domain models

**Step 1: Create sed script for bulk import updates**

```bash
find app/src -name "*.kt" -type f -exec sed -i '' \
  -e 's/import id\.usecase\.noted\.domain\.Note/import id.usecase.noted.shared.note.Note/g' \
  -e 's/import id\.usecase\.noted\.domain\.NoteVisibility/import id.usecase.noted.shared.note.NoteVisibility/g' \
  -e 's/import id\.usecase\.noted\.domain\.NoteHistory/import id.usecase.noted.shared.note.NoteHistory/g' \
  {} +
```

**Step 2: Manual review and fix**

Check these files manually:
- `app/src/main/java/id/usecase/noted/presentation/note/list/NoteListViewModel.kt`
- `app/src/main/java/id/usecase/noted/presentation/note/editor/NoteEditorViewModel.kt`
- `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModel.kt`
- `app/src/main/java/id/usecase/noted/presentation/note/sync/SyncViewModel.kt`

**Step 3: Build and test**

```bash
./gradlew :app:build
./gradlew :app:testDebugUnitTest
```

**Step 4: Commit**

```bash
git add app/
git commit -m "refactor(app): update all imports to use shared domain models"
```

---

## Phase 4: Clean Up and Verification

### Task 4.1: Delete Empty Directories

**Step 1: Remove old backend directories**

```bash
rm -rf backend/src/main/kotlin/id/usecase/backend/note/
rm -rf backend/src/main/kotlin/id/usecase/backend/auth/
rm -rf backend/src/main/kotlin/id/usecase/backend/sync/
```

**Step 2: Verify no remaining references to old packages**

```bash
grep -r "id.usecase.backend.note.domain" backend/src/ || echo "No old references found"
grep -r "id.usecase.backend.auth.domain" backend/src/ || echo "No old references found"
grep -r "id.usecase.backend.note.data" backend/src/ || echo "No old references found"
grep -r "id.usecase.backend.auth.data" backend/src/ || echo "No old references found"
```

**Step 3: Commit**

```bash
git add -A
git commit -m "chore: remove empty directories after refactor"
```

---

### Task 4.2: Full Build and Test Verification

**Step 1: Clean build all modules**

```bash
./gradlew clean
./gradlew :shared:build
./gradlew :backend:build
./gradlew :app:assembleDebug
```

**Step 2: Run all tests**

```bash
./gradlew :shared:test
./gradlew :backend:test
./gradlew :app:testDebugUnitTest
```

Expected: All tests pass

**Step 3: Commit if all tests pass**

```bash
git add -A
git commit -m "test: verify all tests pass after refactor"
```

---

## Phase 5: Documentation

### Task 5.1: Update Architecture Documentation

**Files:**
- Create: `docs/architecture/layer-structure.md`

**Step 1: Document new structure**

```markdown
# Backend Layer Structure

## Overview
Backend now follows layer-first architecture (consistent with Android app):

```
backend/src/main/kotlin/id/usecase/backend/
├── domain/           # Business logic, entities, repository interfaces
│   ├── note/        # Note, NoteRepository, NoteSyncRepository, etc.
│   └── auth/        # User, AuthRepository
├── data/            # Repository implementations
│   ├── note/        # PostgresNoteRepository, InMemoryNoteRepository
│   └── auth/        # PostgresAuthRepository, InMemoryAuthRepository
├── service/         # Business logic/use cases
│   ├── note/        # NoteSharingService, NoteHistoryService
│   ├── auth/        # AuthService
│   └── sync/        # NoteSyncService
├── presentation/    # HTTP routes/controllers
│   ├── note/        # NoteRoutes
│   ├── auth/        # AuthRoutes
│   ├── sync/        # SyncRoutes
│   └── user/        # UserRoutes
└── plugins/         # Ktor configuration
    ├── Routing.kt
    ├── Security.kt
    └── Serialization.kt
```

## Shared Module
Domain models are now shared between app and backend:

```
shared/src/main/kotlin/id/usecase/noted/shared/
├── note/            # Note, NoteVisibility, NoteHistory
├── user/            # User, UserStatistics
├── auth/            # Auth DTOs
└── error/           # DomainError
```
```

**Step 2: Commit**

```bash
git add docs/
git commit -m "docs: add architecture documentation for layer structure"
```

---

## Summary of Changes

### Files Created (New)
- `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteVisibility.kt`
- `shared/src/main/kotlin/id/usecase/noted/shared/note/Note.kt`
- `shared/src/main/kotlin/id/usecase/noted/shared/user/User.kt`
- `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteHistory.kt`
- `shared/src/main/kotlin/id/usecase/noted/shared/error/DomainError.kt`
- `backend/src/main/kotlin/id/usecase/backend/domain/note/*.kt` (5 files)
- `backend/src/main/kotlin/id/usecase/backend/domain/auth/*.kt` (1 file)
- `backend/src/main/kotlin/id/usecase/backend/data/note/*.kt` (6 files, moved)
- `backend/src/main/kotlin/id/usecase/backend/data/auth/*.kt` (2 files, moved)
- `backend/src/main/kotlin/id/usecase/backend/service/note/*.kt` (2 files, moved)
- `backend/src/main/kotlin/id/usecase/backend/service/auth/*.kt` (1 file, moved)
- `backend/src/main/kotlin/id/usecase/backend/service/sync/*.kt` (1 file, moved)
- `backend/src/main/kotlin/id/usecase/backend/presentation/note/*.kt` (1 file, moved)
- `backend/src/main/kotlin/id/usecase/backend/presentation/auth/*.kt` (1 file, moved)
- `backend/src/main/kotlin/id/usecase/backend/presentation/sync/*.kt` (1 file, moved)
- `backend/src/main/kotlin/id/usecase/backend/presentation/user/*.kt` (1 file, moved)

### Files Deleted
- `app/src/main/java/id/usecase/noted/domain/NoteVisibility.kt`
- `app/src/main/java/id/usecase/noted/domain/NoteHistory.kt`
- All old backend domain files
- All old backend data files
- All old backend service files
- All old backend presentation files
- Empty feature directories

### Files Modified
- `app/src/main/java/id/usecase/noted/domain/Note.kt` (refactored)
- `app/src/main/java/id/usecase/noted/data/RoomNoteRepository.kt` (imports)
- All app ViewModels (imports)
- `backend/src/main/kotlin/id/usecase/backend/di/DependencyInjection.kt` (imports)
- `backend/src/main/kotlin/id/usecase/backend/plugins/Routing.kt` (imports)
- All backend service implementations (imports and model references)
- All backend data implementations (imports and model references)
- All test files (imports)

---

## Execution Strategy

This plan should be executed using **parallel sub-agents** for maximum efficiency:

1. **Agent 1**: Phase 0 + Phase 1 (Shared module enhancement)
2. **Agent 2**: Phase 2.1 - 2.2 (Backend domain + data layer)
3. **Agent 3**: Phase 2.3 - 2.4 (Backend service + presentation layer)
4. **Agent 4**: Phase 3 (App migration)
5. **Agent 5**: Phase 4 - 5 (Verification + documentation)

Each agent works on independent parts but must coordinate on shared module changes (Agent 1 must complete before others start).

**Estimated Total Time**: 2-3 hours with parallel execution
**Risk Level**: High (major refactoring)
**Rollback Strategy**: Git worktree allows easy rollback to original state
