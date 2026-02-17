package id.usecase.noted.feature.note.presentation.editor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.input.TextFieldValue
import id.usecase.noted.presentation.note.editor.NoteEditorBlock
import id.usecase.noted.presentation.note.editor.NoteEditorScreen
import id.usecase.noted.presentation.note.editor.NoteEditorState
import id.usecase.noted.ui.theme.NotedTheme
import org.junit.Rule
import org.junit.Test

class NoteEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTopBarBottomAppBarAndSaveFab() {
        composeRule.setContent {
            NotedTheme {
                NoteEditorScreen(
                    state = NoteEditorState(
                        blocks = listOf(
                            NoteEditorBlock.Text(
                                id = 1,
                                value = TextFieldValue("Contoh catatan"),
                            ),
                        ),
                    ),
                    onIntent = {},
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Note Editor").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ambil foto dari kamera").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pilih foto dari galeri").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tag lokasi").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Simpan note").assertIsDisplayed()
        composeRule.onAllNodesWithText("Sisipkan foto sebagai block").assertCountEquals(0)
    }
}
