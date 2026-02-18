# Explore Note Comments & Love Reactions Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add `love` reactions and comments to Explore note detail so other users can interact on notes they can access (especially `LINK_SHARED` and `PUBLIC`), with smooth bottom-bar actions, scroll-to-comments behavior, and stable backend consistency.

**Architecture:** Keep current note/sync architecture intact and add a dedicated engagement slice (contracts, backend repositories/service/routes, Android data repository, and NoteDetail MVI state/effects). Use backend-authoritative counters with optimistic UI only for reaction toggle. Keep one-shot UI actions (scroll/message) in `Effect`.

**Tech Stack:** Kotlin/JVM 21, Ktor, PostgreSQL + in-memory repositories, kotlinx.serialization, Jetpack Compose Material3, Navigation 3, Koin, coroutines test.

---

## Recommended Product/Tech Decisions

1. **Use dedicated engagement endpoints (recommended)**
   - Why: avoids bloating `NoteDto`, keeps sync payload small, and isolates high-churn comment/reaction data from note content sync.

2. **Access policy for interactions**
   - Allow read/write comment + reaction when user can access note via: owner, direct share, `PUBLIC`, or `LINK_SHARED`.
   - This preserves current private behavior while satisfying your `link only + public` requirement.

3. **Scope v1 (YAGNI)**
   - Reactions: single type `LOVE` with toggle semantics.
   - Comments: create + list + pagination cursor (no edit/delete yet).
   - This is enough for "working well" and avoids moderation complexity in first release.

4. **Micro interaction plan**
   - Heart icon spring scale + color transition on toggle.
   - Comment count / love count transitions with `AnimatedContent`.
   - Comment button emits effect to smoothly scroll to comments section (`BringIntoViewRequester`).

---

## API Contract Target (Shared + Backend)

- `GET /api/notes/{noteId}/engagement`
- `PUT /api/notes/{noteId}/reactions/love`
- `DELETE /api/notes/{noteId}/reactions/love`
- `GET /api/notes/{noteId}/comments?limit=20&beforeEpochMillis=<optional>`
- `POST /api/notes/{noteId}/comments`

Expected DTOs:
- `NoteEngagementDto(noteId, loveCount, hasLovedByMe, commentCount)`
- `NoteCommentDto(id, noteId, authorUserId, authorUsername, content, createdAtEpochMillis)`
- `NoteCommentsPageDto(items, nextBeforeEpochMillis)`
- `CreateNoteCommentRequest(content)`

---

## Phase 1: Shared Contracts First (TDD)

### Task 1: Write failing shared serialization tests

**Files:**
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/test/kotlin/id/usecase/noted/shared/note/NoteEngagementContractsSerializationTest.kt`

**Step 1: Add shared test dependency and create failing tests**

- Add `testImplementation(kotlin("test"))` in `shared/build.gradle.kts`.
- Add round-trip serialization tests that reference new DTO names (`NoteEngagementDto`, `NoteCommentDto`, `NoteCommentsPageDto`, `CreateNoteCommentRequest`).

**Step 2: Run test to verify RED**

Run: `./gradlew :shared:test --tests "id.usecase.noted.shared.note.NoteEngagementContractsSerializationTest"`

Expected: FAIL (unresolved references for new engagement contracts).

---

### Task 2: Implement shared engagement contracts

**Files:**
- Create: `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteEngagementContracts.kt`
- Modify: `backend/src/main/resources/openapi/documentation.yaml`

**Step 1: Implement minimal DTOs and enums to satisfy tests**

- Add `@Serializable enum class NoteReactionType { LOVE }` (future-proofing).
- Add the DTOs listed in API Contract Target section.

**Step 2: Run tests to verify GREEN**

Run: `./gradlew :shared:test --tests "id.usecase.noted.shared.note.NoteEngagementContractsSerializationTest"`

Expected: PASS.

**Step 3: Commit**

```bash
git add shared/build.gradle.kts shared/src/test/kotlin/id/usecase/noted/shared/note/NoteEngagementContractsSerializationTest.kt shared/src/main/kotlin/id/usecase/noted/shared/note/NoteEngagementContracts.kt backend/src/main/resources/openapi/documentation.yaml
git commit -m "feat(shared): add note engagement contracts for comments and love reactions"
```

---

## Phase 2: Backend Domain + Service (TDD)

### Task 3: Write failing backend service tests for engagement rules

**Files:**
- Create: `backend/src/test/kotlin/id/usecase/backend/note/service/NoteEngagementServiceTest.kt`

**Step 1: Add service tests for required behavior**

Cover at least:
- `loveToggleIsIdempotentForSameUser`
- `commentCreationRejectsBlankContent`
- `interactionRejectedForInaccessiblePrivateNote`
- `interactionAllowedForLinkSharedAndPublicNote`
- `commentsListUsesDescendingTimeAndCursor`

**Step 2: Run test to verify RED**

Run: `./gradlew :backend:test --tests "id.usecase.backend.note.service.NoteEngagementServiceTest"`

Expected: FAIL (service/repository classes missing).

---

### Task 4: Implement backend domain interfaces + in-memory repositories + service

**Files:**
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/note/NoteCommentRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/note/NoteReactionRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/note/StoredNoteComment.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/domain/note/StoredNoteEngagement.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteCommentRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteReactionRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/service/note/NoteEngagementService.kt`
- Modify: `backend/src/main/kotlin/id/usecase/backend/service/note/NoteSharingService.kt`
- Modify: `backend/src/main/kotlin/id/usecase/backend/domain/note/NoteShareRepository.kt`
- Modify: `backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteShareRepository.kt`
- Modify: `backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteShareRepository.kt`

**Step 1: Implement minimal domain and service logic**

- Add repository methods needed by service (`hasShare`, comment pagination, reaction count/exists).
- Centralize access check in service layer so it supports owner + shared + `PUBLIC` + `LINK_SHARED`.
- Keep validation strict:
  - `content.trim().isNotBlank()`
  - max comment length (recommend 500 chars)
  - positive `limit` with sane cap (recommend 100).

**Step 2: Run tests to verify GREEN**

Run: `./gradlew :backend:test --tests "id.usecase.backend.note.service.NoteEngagementServiceTest"`

Expected: PASS.

**Step 3: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/domain/note/NoteCommentRepository.kt backend/src/main/kotlin/id/usecase/backend/domain/note/NoteReactionRepository.kt backend/src/main/kotlin/id/usecase/backend/domain/note/StoredNoteComment.kt backend/src/main/kotlin/id/usecase/backend/domain/note/StoredNoteEngagement.kt backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteCommentRepository.kt backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteReactionRepository.kt backend/src/main/kotlin/id/usecase/backend/service/note/NoteEngagementService.kt backend/src/main/kotlin/id/usecase/backend/service/note/NoteSharingService.kt backend/src/main/kotlin/id/usecase/backend/domain/note/NoteShareRepository.kt backend/src/main/kotlin/id/usecase/backend/data/note/InMemoryNoteShareRepository.kt backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteShareRepository.kt
git commit -m "feat(backend): add engagement service and in-memory comment/reaction repositories"
```

---

## Phase 3: Backend Routes + Postgres Persistence (TDD)

### Task 5: Write failing backend integration tests for endpoints

**Files:**
- Modify: `backend/src/test/kotlin/id/usecase/backend/ApplicationTest.kt`

**Step 1: Add integration tests for route contract**

Add tests for:
- `authenticatedUserCanLoveAndUnloveAccessibleExploreNote`
- `authenticatedUserCanCreateAndFetchCommentsOnPublicOrLinkSharedNote`
- `privateNoteCommentRequestReturns404Or400ForUnauthorizedUser`

**Step 2: Run tests to verify RED**

Run: `./gradlew :backend:test --tests "id.usecase.backend.ApplicationTest"`

Expected: FAIL (missing routes/storage wiring).

---

### Task 6: Implement Postgres repositories, schema, DI, and routes

**Files:**
- Create: `backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteCommentRepository.kt`
- Create: `backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteReactionRepository.kt`
- Modify: `backend/src/main/kotlin/id/usecase/backend/di/DependencyInjection.kt`
- Modify: `backend/src/main/kotlin/id/usecase/backend/presentation/note/NoteRoutes.kt`
- Modify: `backend/src/main/kotlin/id/usecase/backend/plugins/Routing.kt`
- Modify: `backend/src/main/resources/openapi/documentation.yaml`

**Step 1: Implement SQL persistence + schema migration-safe DDL**

Add `CREATE TABLE IF NOT EXISTS` + indexes:
- `note_comments`
- `note_reactions`

Recommended indexes:
- `idx_note_comments_note_created` on `(note_id, created_at_epoch_millis DESC)`
- `idx_note_reactions_note_type` on `(note_id, reaction_type)`

**Step 2: Add endpoints in `NoteRoutes.kt`**

- `GET /notes/{noteId}/engagement`
- `PUT/DELETE /notes/{noteId}/reactions/love`
- `GET/POST /notes/{noteId}/comments`

**Step 3: Wire repositories + service in DI**

- Register in-memory and postgres variants in both storage modules.
- Inject `NoteEngagementService` into routing.

**Step 4: Run tests to verify GREEN**

Run: `./gradlew :backend:test --tests "id.usecase.backend.ApplicationTest" --tests "id.usecase.backend.note.service.NoteEngagementServiceTest"`

Expected: PASS.

**Step 5: Commit**

```bash
git add backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteCommentRepository.kt backend/src/main/kotlin/id/usecase/backend/data/note/PostgresNoteReactionRepository.kt backend/src/main/kotlin/id/usecase/backend/di/DependencyInjection.kt backend/src/main/kotlin/id/usecase/backend/presentation/note/NoteRoutes.kt backend/src/main/kotlin/id/usecase/backend/plugins/Routing.kt backend/src/main/resources/openapi/documentation.yaml backend/src/test/kotlin/id/usecase/backend/ApplicationTest.kt
git commit -m "feat(backend): expose note engagement endpoints with postgres persistence"
```

---

## Phase 4: Android Data Layer + Explore Navigation (TDD)

### Task 7: Write failing app tests for navigation and detail interactions

**Files:**
- Modify: `app/src/test/java/id/usecase/noted/presentation/note/explore/ExploreViewModelTest.kt`
- Create: `app/src/test/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModelTest.kt`

**Step 1: Add failing tests**

Explore tests:
- clicking explore item emits `NavigateToNoteDetail(noteId)` effect.

Detail tests:
- `LoveClicked` updates state optimistically and rolls back on failure.
- `CommentsClicked` emits scroll effect once.
- `SubmitCommentClicked` validates blank/too-long text.
- successful submit prepends new comment and updates `commentCount`.

**Step 2: Run tests to verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.presentation.note.explore.ExploreViewModelTest" --tests "id.usecase.noted.presentation.note.detail.NoteDetailViewModelTest"`

Expected: FAIL.

---

### Task 8: Implement Explore -> NoteDetail navigation path

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreIntent.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreEffect.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreViewModel.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreScreen.kt`
- Modify: `app/src/main/java/id/usecase/noted/navigation/NoteNavigation.kt`

**Step 1: Add note-click intent/effect and connect callback in screen/navigation**

- `ExploreIntent.NoteClicked(noteId)`
- `ExploreEffect.NavigateToNoteDetail(noteId)`
- Make `ExploreNoteItem` clickable and route through intent.

**Step 2: Run targeted tests**

Run: `./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.presentation.note.explore.ExploreViewModelTest"`

Expected: PASS.

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreIntent.kt app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreEffect.kt app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreViewModel.kt app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreScreen.kt app/src/main/java/id/usecase/noted/navigation/NoteNavigation.kt app/src/test/java/id/usecase/noted/presentation/note/explore/ExploreViewModelTest.kt
git commit -m "feat(app): allow explore items to navigate to note detail"
```

---

### Task 9: Implement Android engagement API + repository + DI

**Files:**
- Create: `app/src/main/java/id/usecase/noted/data/sync/NoteEngagementApi.kt`
- Create: `app/src/main/java/id/usecase/noted/data/sync/KtorNoteEngagementApi.kt`
- Create: `app/src/main/java/id/usecase/noted/data/sync/NoteEngagementRepository.kt`
- Modify: `app/src/main/java/id/usecase/noted/di/AppModule.kt`

**Step 1: Implement API client methods for new backend endpoints**

- Use the same auth style as `KtorNoteHistoryApi` (`bearerAuth`, `appendPathSegments`).
- Map HTTP errors to meaningful messages (`Not found`, `Unauthorized`, fallback generic).

**Step 2: Implement repository with session/token retrieval**

- Return `Result<T>` for VM-friendly error handling.
- Keep methods suspend-based for simplicity in NoteDetail VM.

**Step 3: Wire Koin bindings**

- Add `single<NoteEngagementApi>` and `single<NoteEngagementRepository>`.

**Step 4: Commit**

```bash
git add app/src/main/java/id/usecase/noted/data/sync/NoteEngagementApi.kt app/src/main/java/id/usecase/noted/data/sync/KtorNoteEngagementApi.kt app/src/main/java/id/usecase/noted/data/sync/NoteEngagementRepository.kt app/src/main/java/id/usecase/noted/di/AppModule.kt
git commit -m "feat(app): add engagement api client and repository bindings"
```

---

## Phase 5: NoteDetail MVI + UI + Micro Interactions (TDD)

### Task 10: Expand NoteDetail MVI contracts

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailState.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailIntent.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailEffect.kt`

**Step 1: Add state and one-shot effect contracts**

State additions:
- `engagement: NoteEngagementUi`
- `comments: List<NoteCommentUi>`
- `isCommentsLoading`, `isSendingComment`, `commentInput`, `nextBeforeEpochMillis`, `hasMoreComments`

Intent additions:
- `LoveClicked`, `CommentsClicked`, `CommentInputChanged`, `SubmitCommentClicked`, `LoadMoreComments`, `RefreshEngagement`

Effect addition:
- `ScrollToComments`

**Step 2: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailState.kt app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailIntent.kt app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailEffect.kt
git commit -m "feat(app): define note detail contracts for comments and reactions"
```

---

### Task 11: Implement NoteDetailViewModel engagement logic

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModel.kt`
- Modify: `app/src/test/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModelTest.kt`

**Step 1: Implement RED -> GREEN for tests from Task 7**

- Inject `NoteEngagementRepository`.
- On load: fetch note, engagement summary, first comments page.
- `LoveClicked`: optimistic toggle with rollback on failure.
- `CommentsClicked`: emit `ScrollToComments` effect.
- `SubmitCommentClicked`: validate input, post comment, prepend comment, clear input, update counters.

**Step 2: Run tests to verify GREEN**

Run: `./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.presentation.note.detail.NoteDetailViewModelTest"`

Expected: PASS.

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModel.kt app/src/test/java/id/usecase/noted/presentation/note/detail/NoteDetailViewModelTest.kt
git commit -m "feat(app): implement note detail comment and love reaction state handling"
```

---

### Task 12: Create reusable engagement UI components + motion tokens

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/content/NoteEngagementBar.kt`
- Create: `app/src/main/java/id/usecase/noted/presentation/components/content/NoteCommentsSection.kt`
- Create: `app/src/main/java/id/usecase/noted/ui/theme/MotionTokens.kt`

**Step 1: Build reusable components with previews**

- `NoteEngagementBar` for love/comment action buttons and counters.
- `NoteCommentsSection` for comments list, composer, loading/empty states.
- Add `@Preview` for default/loading/long-list states.

**Step 2: Add stable expressive-like motion specs (no guessed APIs)**

- Centralize spring specs in `MotionTokens.kt`.
- Use these tokens in component animations (`animateFloatAsState`, `AnimatedContent`, `AnimatedVisibility`).

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/content/NoteEngagementBar.kt app/src/main/java/id/usecase/noted/presentation/components/content/NoteCommentsSection.kt app/src/main/java/id/usecase/noted/ui/theme/MotionTokens.kt
git commit -m "feat(app): add reusable engagement components and motion tokens"
```

---

### Task 13: Integrate engagement components into NoteDetailScreen

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailScreen.kt`

**Step 1: Update bottom bar and comments section wiring**

- Add love + comments actions into bottom app bar (required UX).
- Keep existing save/fork behavior unless it breaks spacing; if cramped, prioritize love/comments and keep fork as FAB.
- Place comments section below note information card.

**Step 2: Implement smooth scroll interaction**

- Add `BringIntoViewRequester` to comments section anchor.
- On `ScrollToComments` effect, animate scroll to comments.

**Step 3: Ensure accessibility and interaction quality**

- Content descriptions for action buttons.
- Disable submit button while posting.
- Show inline loading and retry messages.

**Step 4: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailScreen.kt
git commit -m "feat(app): add bottom-bar love/comments and scroll-to-comments behavior"
```

---

## Phase 6: End-to-End Verification and Hardening

### Task 14: Run full relevant verification suite

**Files:**
- No file changes

**Step 1: Run shared tests**

Run: `./gradlew :shared:test`

Expected: PASS.

**Step 2: Run backend tests**

Run: `./gradlew :backend:test`

Expected: PASS.

**Step 3: Run app unit tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: PASS.

**Step 4: Build app + backend**

Run: `./gradlew :app:assembleDebug :backend:build`

Expected: BUILD SUCCESSFUL.

---

### Task 15: Manual QA checklist (must pass before merge)

**Files:**
- No file changes

**Step 1: Explore note detail path**

- From Explore list, tap note card -> opens detail.

**Step 2: Love reaction behavior**

- Tap love icon -> icon animates + count increments.
- Tap again -> decrements (idempotent, no duplicate rows).

**Step 3: Comments behavior**

- Tap comments icon -> screen scrolls to comments section.
- Submit comment -> appears immediately, count updates, input clears.
- Pull/load more comments -> cursor works without duplication.

**Step 4: Access policy checks**

- `PUBLIC` and `LINK_SHARED`: other users can react/comment.
- Private unshared note: returns not accessible.

**Step 5: Regression checks**

- Existing save/fork/copy flows still work.
- No crash when offline/network fails (user gets message + retry path).

---

## Risk Controls

- **Duplicate reactions on retries:** enforce DB uniqueness `(note_id, user_id, reaction_type)`.
- **Unauthorized interaction leak:** single service-level access check reused by all engagement endpoints.
- **Comment spam payload:** max length validation and server-side trim.
- **UI jank:** animate draw-phase properties and keep motion specs centralized.
- **Race conditions in ViewModel:** gate concurrent submit/toggle actions with `isSendingComment`/job cancellation.

---

## Suggested Commit Sequence

1. `feat(shared): add note engagement contracts for comments and love reactions`
2. `feat(backend): add engagement service and in-memory comment/reaction repositories`
3. `feat(backend): expose note engagement endpoints with postgres persistence`
4. `feat(app): allow explore items to navigate to note detail`
5. `feat(app): add engagement api client and repository bindings`
6. `feat(app): define note detail contracts for comments and reactions`
7. `feat(app): implement note detail comment and love reaction state handling`
8. `feat(app): add reusable engagement components and motion tokens`
9. `feat(app): add bottom-bar love/comments and scroll-to-comments behavior`
