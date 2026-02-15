package id.usecase.noted.feature.note.data

import android.content.Context
import id.usecase.noted.feature.note.data.local.NoteDatabase

object NoteRepositoryProvider {
    @Volatile
    private var repository: NoteRepository? = null

    fun provide(context: Context): NoteRepository {
        return repository ?: synchronized(this) {
            repository ?: RoomNoteRepository(
                noteDao = NoteDatabase.getInstance(context).noteDao(),
            ).also { newRepository ->
                repository = newRepository
            }
        }
    }
}
