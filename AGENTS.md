# AGENTS.md

## Purpose
- This file is the operating guide for coding agents working in `Noted`.
- Follow this before making code changes, running commands, or writing docs.
- Prefer repository conventions over generic framework defaults.

## Repository Snapshot
- Monorepo with three Gradle modules: `:app`, `:backend`, `:shared`.
- `app/` = Android app (Jetpack Compose, Room, Navigation 3, Ktor client, DataStore).
- `backend/` = Ktor server (JWT auth, repository pattern, PostgreSQL/in-memory mode).
- `shared/` = cross-module `kotlinx.serialization` contracts for auth + note/sync APIs.
- Java/Kotlin target: 17 across modules.

## Rule Files Discovery (Cursor/Copilot)
- No `.cursorrules` found.
- No `.cursor/rules/` directory found.
- No `.github/copilot-instructions.md` found.
- If any of these files appear later, treat them as higher-priority local instructions and merge them into this guide.

## Tooling and Environment
- Build system: Gradle (`./gradlew`).
- Android SDK required for `:app` build/lint/test tasks.
- Backend database for local dev: Docker Compose in `backend/docker-compose.yml`.
- Default backend HTTP port: `8080`.
- Default local Postgres port: `5433`.
- Emulator app->backend base URL currently expects `http://10.0.2.2:8080`.

## High-Value Commands

### Project Build
```bash
./gradlew build
./gradlew :shared:build
./gradlew :backend:build
./gradlew :app:assembleDebug
```

### Lint / Static Checks
```bash
./gradlew :app:lintDebug
./gradlew :app:lint
```
- There is no configured `ktlint`, `detekt`, or `spotless` task right now.
- Use Kotlin official formatter settings in IDE/CI-compatible style.

### Tests (All)
```bash
./gradlew :backend:test :app:testDebugUnitTest
./gradlew :backend:test
./gradlew :app:testDebugUnitTest
```

### Tests (Single Test Class)
```bash
./gradlew :backend:test --tests "id.usecase.ApplicationTest"
./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.feature.note.presentation.list.NoteListViewModelTest"
```

### Tests (Single Test Method)
```bash
./gradlew :backend:test --tests "id.usecase.ApplicationTest.testSyncPushAndPull"
./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.feature.note.presentation.list.NoteListViewModelTest.loginSubmitCallsLogin"
```

### Backend Runtime
```bash
docker compose -f backend/docker-compose.yml up -d
docker compose -f backend/docker-compose.yml ps
./gradlew :backend:run
docker compose -f backend/docker-compose.yml down
```

## Architecture and Boundaries
- Keep API contracts in `shared/`; both app and backend must consume these DTOs.
- Do not duplicate request/response models separately in app/backend.
- Backend must preserve repository pattern:
  - Repository interfaces define persistence contracts.
  - Services contain business logic and validation.
  - Routing stays thin and delegates to services.
- App feature layout follows `data/`, `domain/`, `presentation/`.
- ViewModel-driven presentation uses state + one-shot effect flows.

## Kotlin Style Conventions

### Imports
- No wildcard imports.
- Remove unused imports.
- Keep imports grouped by package origin (standard/Android/project/kotlinx/third-party) in stable order.
- Use import aliasing only when naming collision is real.

### Formatting
- 4-space indentation, no tabs.
- Keep trailing commas for multiline parameter lists and data classes.
- Prefer readable multiline wrapping over dense one-liners.
- Keep braces and `when` branches in standard Kotlin style.
- Respect `kotlin.code.style=official` from `gradle.properties`.

### Types and Nullability
- Use explicit types on public APIs and important state holders.
- Prefer immutable `val`; use `var` only for mutable state that must change.
- Model optional data with nullable types; avoid sentinel strings/numbers.
- Validate assumptions early with `require`, `check`, or explicit guard clauses.
- Avoid `!!`; use safe calls, Elvis (`?:`), or early returns.

### Naming
- Packages: lowercase dot-separated (e.g., `id.usecase.noted.feature.note.data.sync`).
- Classes/interfaces/objects: PascalCase.
- Functions/properties/locals: camelCase.
- Constants: UPPER_SNAKE_CASE.
- Boolean names should read as predicates (`isLoading`, `isLoggedIn`, `hasAccess`).
- Test names should describe behavior (`loginSubmitCallsLogin`, `testSyncPushAndPull`).

## Android App Conventions
- Compose screens are split root/content style (e.g., `ScreenRoot` collects from ViewModel).
- Intents are sealed interfaces; user actions flow through `onIntent(...)`.
- Keep UI state in immutable `data class` state objects.
- Expose state as `StateFlow`; keep mutable backing fields private.
- Use `Channel` + `receiveAsFlow()` for one-time effects (navigation/snackbar).
- Repository handles local-first behavior and sync orchestration.
- Room entities use snake_case column names via `@ColumnInfo` when persisted names differ.
- Add/maintain migrations when schema version changes.

## Backend Conventions
- Ktor plugins configured in dedicated functions (`configureSerialization`, `configureSecurity`, `configureRouting`).
- Route handlers should extract auth/user context and delegate business logic to services.
- JWT subject (`principal.payload.subject`) is the server-trusted `userId`.
- Sync/auth routes must not trust client-supplied user identifiers.
- Use structured logging with placeholders (`logger.info("x={} y={}", x, y)`).
- Keep SQL constants centralized and descriptive.
- When using transactions, always rollback on failure and restore connection state.

## Error Handling and Resilience
- Validate external inputs at boundaries (HTTP request parsing, repository entry points).
- Backend maps:
  - `IllegalArgumentException` -> HTTP 400
  - auth failures -> HTTP 401
  - unhandled exceptions -> HTTP 500
- App ViewModels convert failures into user-visible messages via effects.
- Retry only transient failures; use bounded exponential backoff.
- Guard concurrent sync execution with `Mutex` or equivalent.

## Testing Guidelines
- Prefer fast unit tests for domain/presentation logic.
- Use `kotlinx.coroutines.test` utilities (`runTest`, `advanceUntilIdle`).
- App tests use fakes/stubs over heavy mocking frameworks.
- Backend endpoint tests should use `testApplication` and shared DTO serialization.
- For bug fixes: add a regression test that fails before implementation and passes after.
- When changing contracts, update tests in all affected modules (`shared`, `backend`, `app`).

## Documentation and File Placement
- Keep a single `README.md` at repository root.
- Do not create module-level READMEs unless explicitly requested later.
- Put planning/design docs under `docs/` (for example `docs/plans/`).
- Keep `AGENTS.md` in the root so agent tooling can discover it quickly.

## Safety and Change Hygiene
- Never commit secrets (API keys, tokens, credentials, `.env` data).
- Do not force push or rewrite shared history unless explicitly asked.
- Make minimal, focused changes; avoid opportunistic refactors.
- If touching multiple layers, keep contracts and usage synchronized in one change set.
- Before finalizing, run the narrowest relevant verification command(s) and report results clearly.
