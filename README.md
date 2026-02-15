# Noted

Noted is a Kotlin monorepo containing an Android app, a Ktor backend, and shared API contracts.

## Modules

- `app/` - Android app (Jetpack Compose, Room, Navigation 3, Ktor client, DataStore)
- `backend/` - Ktor server (JWT auth, repository pattern, PostgreSQL/in-memory storage)
- `shared/` - Shared `kotlinx.serialization` contracts for auth, notes, and sync

## Prerequisites

- JDK 17
- Android SDK (for app build/test/lint)
- Docker (for local PostgreSQL backend runtime)

## Build

```bash
./gradlew build
./gradlew :shared:build
./gradlew :backend:build
./gradlew :app:assembleDebug
```

## Lint

```bash
./gradlew :app:lintDebug
./gradlew :app:lint
```

## Test

```bash
./gradlew :backend:test :app:testDebugUnitTest
./gradlew :backend:test
./gradlew :app:testDebugUnitTest
```

Single test examples:

```bash
./gradlew :backend:test --tests "id.usecase.ApplicationTest"
./gradlew :backend:test --tests "id.usecase.ApplicationTest.testSyncPushAndPull"
./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.feature.note.presentation.list.NoteListViewModelTest"
./gradlew :app:testDebugUnitTest --tests "id.usecase.noted.feature.note.presentation.list.NoteListViewModelTest.loginSubmitCallsLogin"
```

## Run Backend Locally

```bash
docker compose -f backend/docker-compose.yml up -d
docker compose -f backend/docker-compose.yml ps
./gradlew :backend:run
docker compose -f backend/docker-compose.yml down
```

Default ports:

- Backend API: `http://localhost:8080`
- Postgres: `localhost:5433`

Android emulator should use `http://10.0.2.2:8080` for backend access.

## Project Conventions

- Shared API DTOs live in `shared/` and are consumed by both app and backend.
- Backend route handlers stay thin; services hold business logic.
- App presentation uses state + one-shot effects from ViewModel.
- Repository root contains project docs (`README.md`, `AGENTS.md`).
