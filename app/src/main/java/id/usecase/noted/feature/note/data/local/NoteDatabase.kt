package id.usecase.noted.feature.note.data.local

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
    ],
    version = 2,
    exportSchema = false,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    abstract fun syncCursorDao(): SyncCursorDao

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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { built ->
                        instance = built
                    }
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
