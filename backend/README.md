# Noted Backend

Ktor backend module for the Noted monorepo.

## Architecture

This backend uses a repository pattern:

- `NoteRepository` -> note storage abstraction
- `NoteShareRepository` -> note sharing storage abstraction
- `NoteSharingService` -> use case orchestration for create/read/share flows

Default runtime implementation now uses PostgreSQL-backed repositories.

For tests and local-only quick checks, `storage.mode=memory` is still supported.

## Shared Contracts

Request/response models are defined in the shared module:

- `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteContracts.kt`
- `shared/src/main/kotlin/id/usecase/noted/shared/note/NoteSyncContracts.kt`
- `shared/src/main/kotlin/id/usecase/noted/shared/auth/AuthContracts.kt`

## API Endpoints

- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/notes` (requires `Authorization: Bearer <accessToken>`)
- `GET /api/notes/{noteId}` (requires bearer token)
- `GET /api/notes` (requires bearer token)
- `POST /api/notes/{noteId}/share` (requires bearer token)
- `GET /api/notes/shared-with-me` (requires bearer token)
- `POST /api/sync/push` (requires bearer token)
- `GET /api/sync/pull?cursor=...` (requires bearer token)

## Offline-First Sync Notes

- Sync is user-scoped by JWT subject (server-trusted user identity) and `deviceId` from client.
- Server supports idempotent push by `operationId` and optimistic conflict checks via `baseVersion`.
- Pull uses cursor-based incremental changes and includes tombstones (`deletedAtEpochMillis`) for safe deletes.

## Authentication Notes

- Access tokens are JWTs issued by `/api/auth/register` and `/api/auth/login`.
- Authenticated routes read user id from JWT subject; client must not send user id in sync payload.
- Passwords are stored as bcrypt hashes.

## Run and Test

From repository root:

1. Start dedicated Postgres container group:

   - `docker compose -f backend/docker-compose.yml up -d`

2. Run backend:

   - `./gradlew :backend:run`

3. Run tests:

   - `./gradlew :backend:test`

4. Stop containers when done:

   - `docker compose -f backend/docker-compose.yml down`
