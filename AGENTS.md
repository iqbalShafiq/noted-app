# AGENTS.md

## Purpose
- Operating guide for coding agents working in `Noted`.
- Follow this before making changes, running commands, or writing docs.
- Prefer repository conventions over generic framework defaults.

## Repository Snapshot
- Monorepo with three Gradle modules: `:app`, `:backend`, `:shared`.
- `app/` = Android app (Jetpack Compose, Room, Navigation 3, Ktor client, DataStore).
- `backend/` = Ktor server (JWT auth, repository pattern, PostgreSQL/in-memory mode).
- `shared/` = cross-module `kotlinx.serialization` contracts for auth + note/sync APIs.
- Java/Kotlin target: 17 across modules.

## High-Value Commands

### Build
```bash
./gradlew build
./gradlew :app:assembleDebug
./gradlew :backend:build
```

### Lint
```bash
./gradlew :app:lintDebug
```
- No configured `ktlint`, `detekt`, or `spotless`. Use IDE Kotlin formatter.

### Tests (All)
```bash
./gradlew :backend:test :app:testDebugUnitTest
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
./gradlew :backend:run
```

## Code Style

### Imports
- No wildcard imports. Remove unused imports.
- Group by origin: standard/Android/project/kotlinx/third-party.
- Use aliasing only for real naming collisions.

### Formatting
- 4-space indentation, no tabs.
- Trailing commas for multiline parameters and data classes.
- Respect `kotlin.code.style=official` from `gradle.properties`.

### Types
- Explicit types on public APIs.
- Prefer immutable `val`; use `var` only for mutable state.
- Model optional data with nullable types.
- Avoid `!!`; use safe calls, Elvis (`?:`), or early returns.

### Naming
- Packages: lowercase dot-separated.
- Classes: PascalCase, functions: camelCase, constants: UPPER_SNAKE_CASE.
- Boolean predicates: `isLoading`, `hasAccess`.
- Test names describe behavior: `loginSubmitCallsLogin`.

## Reusable Components (CRITICAL)

**ALWAYS CHECK** `app/src/main/java/id/usecase/noted/presentation/components/` before creating new UI components:

### Available Components
- `feedback/` - LoadingState, ErrorState, EmptyState
- `navigation/` - NotedTopAppBar, NotedBottomAppBar
- `content/` - InfoRow, NoteCard
- `auth/` - AuthFormLayout, AuthHeader, AuthTextField, AuthSubmitButton

### When Adding New Components
1. **Check if similar exists first** - Don't duplicate patterns
2. **Follow existing patterns** - Use Material3, add @Preview, proper KDoc
3. **Place in correct package** - Group by function (feedback/navigation/content/auth)
4. **Keep them generic** - No business logic, configurable via parameters
5. **Add Previews** - Every component needs @Preview for design verification

### Migration Rule
- When modifying screens, **replace inline components** with reusable ones
- Delete inline implementations after migration
- Update imports: `id.usecase.noted.presentation.components.*`

## Android Architecture
- Feature layout: `data/`, `domain/`, `presentation/`.
- Compose screens: split root/content style (`ScreenRoot` collects from ViewModel).
- Intents: sealed interfaces flowing through `onIntent(...)`.
- UI state: immutable `data class` exposed as `StateFlow`.
- Effects: `Channel` + `receiveAsFlow()` for navigation/snackbar.
- Repository handles local-first behavior and sync.
- Room entities: snake_case column names via `@ColumnInfo`.

## Backend Architecture
- Repository pattern: interfaces define persistence, services contain logic.
- Routing stays thin, delegates to services.
- JWT subject = server-trusted `userId`.
- Ktor plugins: dedicated `configureX()` functions.
- Logging: structured with placeholders (`logger.info("x={}", x)`).

## Error Handling
- Validate inputs at boundaries.
- Backend exceptions → HTTP status: `IllegalArgumentException` → 400, auth failures → 401.
- App ViewModels convert failures to user-visible effects.
- Guard concurrent sync with `Mutex`.

## Testing
- Prefer fast unit tests for domain/presentation.
- Use `kotlinx.coroutines.test` (`runTest`, `advanceUntilIdle`).
- Fakes/stubs over heavy mocking.
- Add regression tests for bug fixes.
- Update tests in all affected modules when changing contracts.

## Verification
- Before finalizing: run narrowest relevant command(s).
- Report results clearly in commit messages.
- Never commit secrets or force push shared history.
