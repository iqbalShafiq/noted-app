package id.usecase.noted.presentation.components.camera

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import id.usecase.noted.ui.theme.NotedTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cameraIconButton_displaysAndHandlesClick() {
        var clicked = false

        composeRule.setContent {
            NotedTheme {
                CameraIconButton(
                    icon = Icons.Default.CameraAlt,
                    contentDescription = "Switch camera",
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Switch camera")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun cameraShutterButton_handlesClick() {
        var clicked = false
        val shutterContentDescription = "Test shutter"

        composeRule.setContent {
            NotedTheme {
                CameraShutterButton(
                    onClick = { clicked = true },
                    contentDescription = shutterContentDescription,
                )
            }
        }

        composeRule.onNodeWithContentDescription(shutterContentDescription)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun cameraIconButton_selectedState_renders() {
        composeRule.setContent {
            NotedTheme {
                CameraIconButton(
                    icon = Icons.Default.CameraAlt,
                    contentDescription = "Grid",
                    onClick = {},
                    isSelected = true,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Grid")
            .assertIsDisplayed()
            .assertIsSelected()
    }
}
