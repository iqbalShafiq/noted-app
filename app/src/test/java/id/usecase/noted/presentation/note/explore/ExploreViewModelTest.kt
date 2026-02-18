package id.usecase.noted.presentation.note.explore

import id.usecase.noted.data.sync.ExploreRepository
import id.usecase.noted.presentation.MainDispatcherRule
import id.usecase.noted.shared.note.NoteDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initWhenRepositoryThrowsSetsErrorState() = runTest {
        val viewModel = ExploreViewModel(exploreRepository = ThrowingExploreRepository())

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Login diperlukan untuk explore", viewModel.state.value.errorMessage)
    }

    @Test
    fun noteClickedEmitsNavigateToNoteDetailEffect() = runTest {
        val viewModel = ExploreViewModel(exploreRepository = ThrowingExploreRepository())
        val firstEffect = async { viewModel.effect.first() }

        viewModel.onIntent(ExploreIntent.NoteClicked(noteId = "note-123"))
        advanceUntilIdle()

        assertEquals(
            ExploreEffect.NavigateToNoteDetail(noteId = "note-123"),
            firstEffect.await(),
        )
    }
}

private class ThrowingExploreRepository : ExploreRepository {
    override fun exploreNotes(limit: Int): Flow<Result<List<NoteDto>>> = flow {
        throw IllegalStateException("Login diperlukan untuk explore")
    }

    override fun searchExploreNotes(query: String, limit: Int): Flow<Result<List<NoteDto>>> = flow {
        emit(Result.success(emptyList()))
    }

    override fun getNoteById(noteId: String): Flow<Result<NoteDto>> = flow {
        throw UnsupportedOperationException("Not used in this test")
    }
}
