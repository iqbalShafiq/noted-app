package id.usecase.noted.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteEntity::class,
        SyncCursorEntity::class,
        NoteHistoryEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    abstract fun syncCursorDao(): SyncCursorDao

    abstract fun noteHistoryDao(): NoteHistoryDao

    companion object {
        @Volatile
        private var instance: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { built ->
                        instance = built
                    }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        note_id TEXT NOT NULL,
                        owner_user_id TEXT NOT NULL,
                        content TEXT NOT NULL,
                        viewed_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_history_note_id ON note_history(note_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_history_viewed_at ON note_history(viewed_at)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN forked_from TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN visibility TEXT NOT NULL DEFAULT 'PRIVATE'")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN note_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE notes SET note_id = 'legacy-' || id WHERE note_id = ''")

                db.execSQL("ALTER TABLE notes ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE notes SET updated_at = created_at WHERE updated_at = 0")

                db.execSQL("ALTER TABLE notes ADD COLUMN owner_user_id TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                db.execSQL("ALTER TABLE notes ADD COLUMN server_version INTEGER")
                db.execSQL("ALTER TABLE notes ADD COLUMN deleted_at INTEGER")
                db.execSQL("ALTER TABLE notes ADD COLUMN sync_error_message TEXT")

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notes_note_id ON notes(note_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_owner_user_id ON notes(owner_user_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_sync_status ON notes(sync_status)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_cursors (
                        user_id TEXT NOT NULL PRIMARY KEY,
                        cursor INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
