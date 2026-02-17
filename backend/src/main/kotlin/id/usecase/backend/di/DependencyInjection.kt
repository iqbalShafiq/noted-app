package id.usecase.backend.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import id.usecase.backend.auth.security.JwtConfig
import id.usecase.backend.auth.security.JwtService
import id.usecase.backend.auth.security.toJwtConfig
import id.usecase.backend.data.auth.InMemoryAuthRepository
import id.usecase.backend.data.auth.PostgresAuthRepository
import id.usecase.backend.data.note.InMemoryNoteHistoryRepository
import id.usecase.backend.data.note.InMemoryNoteRepository
import id.usecase.backend.data.note.InMemoryNoteShareRepository
import id.usecase.backend.data.note.PostgresNoteHistoryRepository
import id.usecase.backend.data.note.PostgresNoteRepository
import id.usecase.backend.data.note.PostgresNoteShareRepository
import id.usecase.backend.domain.auth.AuthRepository
import id.usecase.backend.domain.note.NoteHistoryRepository
import id.usecase.backend.domain.note.NoteRepository
import id.usecase.backend.domain.note.NoteShareRepository
import id.usecase.backend.domain.note.NoteSyncRepository
import id.usecase.backend.service.auth.AuthService
import id.usecase.backend.service.note.NoteHistoryService
import id.usecase.backend.service.note.NoteSharingService
import id.usecase.backend.service.sync.NoteSyncService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.config.ApplicationConfig
import javax.sql.DataSource
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection(
    storageModeOverride: String? = null,
) {
    val storageMode = resolveStorageMode(storageModeOverride)

    install(Koin) {
        slf4jLogger()
        modules(
            commonModule(environment.config),
            when (storageMode) {
                StorageMode.MEMORY -> inMemoryStorageModule()
                StorageMode.POSTGRES -> postgresStorageModule()
            },
        )
    }

    when (storageMode) {
        StorageMode.MEMORY -> log.info("Using in-memory storage mode")
        StorageMode.POSTGRES -> {
            val postgresConfig = getKoin().get<PostgresConfig>()
            log.info("Using PostgreSQL storage mode at {}", postgresConfig.jdbcUrl)
            monitor.subscribe(ApplicationStopped) {
                runCatching { getKoin().get<HikariDataSource>() }
                    .onSuccess { dataSource -> dataSource.close() }
            }
        }
    }
}

private fun commonModule(config: ApplicationConfig) = module {
    single { config }
    single<JwtConfig> { get<ApplicationConfig>().toJwtConfig() }
    single { JwtService(get()) }
    single {
        NoteSharingService(
            noteRepository = get(),
            noteShareRepository = get(),
        )
    }
    single { NoteSyncService(noteSyncRepository = get()) }
    single {
        NoteHistoryService(
            noteHistoryRepository = get(),
            noteRepository = get(),
        )
    }
    single {
        AuthService(
            authRepository = get(),
            jwtService = get(),
        )
    }
}

private fun inMemoryStorageModule() = module {
    single { InMemoryNoteRepository() }
    single<NoteRepository> { get<InMemoryNoteRepository>() }
    single<NoteSyncRepository> { get<InMemoryNoteRepository>() }
    single<NoteShareRepository> { InMemoryNoteShareRepository() }
    single<NoteHistoryRepository> { InMemoryNoteHistoryRepository() }
    single<AuthRepository> { InMemoryAuthRepository() }
}

private fun postgresStorageModule() = module {
    single<PostgresConfig> { get<ApplicationConfig>().toPostgresConfig() }
    single<HikariDataSource> {
        val dataSource = createPostgresDataSource(get())
        initializeSchema(dataSource)
        dataSource
    }
    single<DataSource> { get<HikariDataSource>() }
    single { PostgresNoteRepository(get()) }
    single<NoteRepository> { get<PostgresNoteRepository>() }
    single<NoteSyncRepository> { get<PostgresNoteRepository>() }
    single<NoteShareRepository> { PostgresNoteShareRepository(get()) }
    single<NoteHistoryRepository> { PostgresNoteHistoryRepository(get()) }
    single<AuthRepository> { PostgresAuthRepository(get()) }
}

private enum class StorageMode {
    MEMORY,
    POSTGRES,
}

private fun Application.resolveStorageMode(storageModeOverride: String?): StorageMode {
    val rawMode = storageModeOverride
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: environment.config.propertyOrNull("storage.mode")
            ?.getString()
            ?.trim()
            ?.lowercase()
            ?: "postgres"

    return when (rawMode) {
        "memory" -> StorageMode.MEMORY
        "postgres" -> StorageMode.POSTGRES
        else -> throw IllegalArgumentException("Unsupported storage.mode '$rawMode'")
    }
}

private data class PostgresConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int,
)

private fun ApplicationConfig.toPostgresConfig(): PostgresConfig {
    return PostgresConfig(
        jdbcUrl = readString(
            path = "postgres.jdbcUrl",
            envKey = "POSTGRES_JDBC_URL",
            fallback = "jdbc:postgresql://localhost:5433/noted",
        ),
        username = readString(
            path = "postgres.username",
            envKey = "POSTGRES_USERNAME",
            fallback = "noted_user",
        ),
        password = readString(
            path = "postgres.password",
            envKey = "POSTGRES_PASSWORD",
            fallback = "noted_password",
        ),
        maximumPoolSize = readInt(
            path = "postgres.maximumPoolSize",
            envKey = "POSTGRES_MAX_POOL_SIZE",
            fallback = 8,
        ),
    )
}

private fun ApplicationConfig.readString(path: String, envKey: String, fallback: String): String {
    val fromEnv = System.getenv(envKey)?.trim().orEmpty()
    if (fromEnv.isNotBlank()) {
        return fromEnv
    }

    val fromConfig = propertyOrNull(path)?.getString()?.trim().orEmpty()
    if (fromConfig.isNotBlank()) {
        return fromConfig
    }

    return fallback
}

private fun ApplicationConfig.readInt(path: String, envKey: String, fallback: Int): Int {
    val fromEnv = System.getenv(envKey)?.trim()?.toIntOrNull()
    if (fromEnv != null) {
        return fromEnv
    }

    val fromConfig = propertyOrNull(path)?.getString()?.trim()?.toIntOrNull()
    if (fromConfig != null) {
        return fromConfig
    }

    return fallback
}

private fun createPostgresDataSource(config: PostgresConfig): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.username
        password = config.password
        maximumPoolSize = config.maximumPoolSize
        minimumIdle = 1
        connectionTimeout = 10_000
        idleTimeout = 60_000
        maxLifetime = 30 * 60_000
        driverClassName = "org.postgresql.Driver"
    }

    return HikariDataSource(hikariConfig)
}

private fun initializeSchema(dataSource: DataSource) {
    dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeUpdate(CREATE_NOTES_TABLE_SQL)
            statement.executeUpdate(CREATE_USERS_TABLE_SQL)
            statement.executeUpdate(ALTER_NOTES_ADD_UPDATED_SQL)
            statement.executeUpdate(ALTER_NOTES_ADD_DELETED_SQL)
            statement.executeUpdate(ALTER_NOTES_ADD_VERSION_SQL)
            statement.executeUpdate(BACKFILL_NOTES_UPDATED_SQL)
            statement.executeUpdate(BACKFILL_NOTES_VERSION_SQL)
            statement.executeUpdate(CREATE_NOTES_OWNER_INDEX_SQL)
            statement.executeUpdate(CREATE_NOTE_SHARES_TABLE_SQL)
            statement.executeUpdate(CREATE_NOTE_SHARES_RECIPIENT_INDEX_SQL)
            statement.executeUpdate(CREATE_SYNC_EVENTS_TABLE_SQL)
            statement.executeUpdate(DROP_SYNC_EVENTS_FK_SQL)
            statement.executeUpdate(CREATE_SYNC_EVENTS_OWNER_CURSOR_INDEX_SQL)
            runCatching { statement.executeUpdate(ALTER_USERS_ADD_DISPLAY_NAME_SQL) }
            runCatching { statement.executeUpdate(ALTER_USERS_ADD_BIO_SQL) }
            runCatching { statement.executeUpdate(ALTER_USERS_ADD_PROFILE_PICTURE_URL_SQL) }
            runCatching { statement.executeUpdate(ALTER_USERS_ADD_EMAIL_SQL) }
            runCatching { statement.executeUpdate(ALTER_USERS_ADD_LAST_LOGIN_SQL) }
            runCatching { statement.executeUpdate(ALTER_USERS_ADD_UPDATED_AT_SQL) }
            runCatching { statement.executeUpdate(ALTER_NOTES_ADD_VISIBILITY_SQL) }
            runCatching { statement.executeUpdate(BACKFILL_NOTES_VISIBILITY_SQL) }
            runCatching { statement.executeUpdate(CREATE_NOTES_VISIBILITY_INDEX_SQL) }
            runCatching { statement.executeUpdate(CREATE_NOTE_HISTORY_TABLE_SQL) }
            runCatching { statement.executeUpdate(CREATE_NOTE_HISTORY_USER_INDEX_SQL) }
        }
    }
}

private const val CREATE_NOTES_TABLE_SQL = """
    CREATE TABLE IF NOT EXISTS notes (
        id TEXT PRIMARY KEY,
        owner_user_id TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at_epoch_millis BIGINT NOT NULL
    )
"""

private const val CREATE_USERS_TABLE_SQL = """
    CREATE TABLE IF NOT EXISTS users (
        user_id TEXT PRIMARY KEY,
        username TEXT NOT NULL UNIQUE,
        password_hash TEXT NOT NULL,
        created_at_epoch_millis BIGINT NOT NULL,
        display_name TEXT,
        bio TEXT,
        profile_picture_url TEXT,
        email TEXT,
        last_login_at_epoch_millis BIGINT,
        updated_at_epoch_millis BIGINT NOT NULL DEFAULT 0
    )
"""

private const val ALTER_USERS_ADD_DISPLAY_NAME_SQL = """
    ALTER TABLE users
    ADD COLUMN IF NOT EXISTS display_name TEXT
"""

private const val ALTER_USERS_ADD_BIO_SQL = """
    ALTER TABLE users
    ADD COLUMN IF NOT EXISTS bio TEXT
"""

private const val ALTER_USERS_ADD_PROFILE_PICTURE_URL_SQL = """
    ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_picture_url TEXT
"""

private const val ALTER_USERS_ADD_EMAIL_SQL = """
    ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email TEXT
"""

private const val ALTER_USERS_ADD_LAST_LOGIN_SQL = """
    ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_login_at_epoch_millis BIGINT
"""

private const val ALTER_USERS_ADD_UPDATED_AT_SQL = """
    ALTER TABLE users
    ADD COLUMN IF NOT EXISTS updated_at_epoch_millis BIGINT NOT NULL DEFAULT 0
"""

private const val ALTER_NOTES_ADD_UPDATED_SQL = """
    ALTER TABLE notes
    ADD COLUMN IF NOT EXISTS updated_at_epoch_millis BIGINT
"""

private const val ALTER_NOTES_ADD_DELETED_SQL = """
    ALTER TABLE notes
    ADD COLUMN IF NOT EXISTS deleted_at_epoch_millis BIGINT
"""

private const val ALTER_NOTES_ADD_VERSION_SQL = """
    ALTER TABLE notes
    ADD COLUMN IF NOT EXISTS version BIGINT
"""

private const val BACKFILL_NOTES_UPDATED_SQL = """
    UPDATE notes
    SET updated_at_epoch_millis = created_at_epoch_millis
    WHERE updated_at_epoch_millis IS NULL
"""

private const val BACKFILL_NOTES_VERSION_SQL = """
    UPDATE notes
    SET version = 1
    WHERE version IS NULL
"""

private const val CREATE_NOTES_OWNER_INDEX_SQL = """
    CREATE INDEX IF NOT EXISTS idx_notes_owner
    ON notes(owner_user_id, created_at_epoch_millis DESC)
"""

private const val CREATE_NOTE_SHARES_TABLE_SQL = """
    CREATE TABLE IF NOT EXISTS note_shares (
        note_id TEXT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
        recipient_user_id TEXT NOT NULL,
        shared_at_epoch_millis BIGINT NOT NULL,
        PRIMARY KEY (note_id, recipient_user_id)
    )
"""

private const val CREATE_NOTE_SHARES_RECIPIENT_INDEX_SQL = """
    CREATE INDEX IF NOT EXISTS idx_note_shares_recipient
    ON note_shares(recipient_user_id, shared_at_epoch_millis DESC)
"""

private const val CREATE_SYNC_EVENTS_TABLE_SQL = """
    CREATE TABLE IF NOT EXISTS note_sync_events (
        cursor BIGSERIAL PRIMARY KEY,
        owner_user_id TEXT NOT NULL,
        operation_id TEXT NOT NULL,
        note_id TEXT NOT NULL,
        created_at_epoch_millis BIGINT NOT NULL,
        UNIQUE (owner_user_id, operation_id)
    )
"""

private const val DROP_SYNC_EVENTS_FK_SQL = """
    ALTER TABLE note_sync_events
    DROP CONSTRAINT IF EXISTS note_sync_events_note_id_fkey
"""

private const val CREATE_SYNC_EVENTS_OWNER_CURSOR_INDEX_SQL = """
    CREATE INDEX IF NOT EXISTS idx_note_sync_events_owner_cursor
    ON note_sync_events(owner_user_id, cursor)
"""

private const val ALTER_NOTES_ADD_VISIBILITY_SQL = """
    ALTER TABLE notes
    ADD COLUMN IF NOT EXISTS visibility TEXT NOT NULL DEFAULT 'PRIVATE'
"""

private const val BACKFILL_NOTES_VISIBILITY_SQL = """
    UPDATE notes
    SET visibility = 'PRIVATE'
    WHERE visibility IS NULL
"""

private const val CREATE_NOTES_VISIBILITY_INDEX_SQL = """
    CREATE INDEX IF NOT EXISTS idx_notes_visibility
    ON notes(visibility, created_at_epoch_millis DESC)
    WHERE deleted_at_epoch_millis IS NULL
"""

private const val CREATE_NOTE_HISTORY_TABLE_SQL = """
    CREATE TABLE IF NOT EXISTS note_history (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        note_id TEXT NOT NULL,
        note_owner_id TEXT NOT NULL,
        content TEXT NOT NULL,
        viewed_at_epoch_millis BIGINT NOT NULL,
        UNIQUE (user_id, note_id)
    )
"""

private const val CREATE_NOTE_HISTORY_USER_INDEX_SQL = """
    CREATE INDEX IF NOT EXISTS idx_note_history_user_viewed
    ON note_history(user_id, viewed_at_epoch_millis DESC)
"""