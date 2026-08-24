package io.github.jukerupup.mydictionary.ui.lookup

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import io.github.jukerupup.mydictionary.ui.components.StatusKind
import io.github.jukerupup.mydictionary.ui.components.StatusPanel
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LookupPrimitiveCharacterizationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun approvedOfflineStatusPrimitiveKeepsRecoveryActionAndSemanticTag() {
        composeRule.setContent {
            MyDictionaryTheme {
                StatusPanel(
                    kind = StatusKind.Offline,
                    title = "You're offline",
                    message = "Reconnect, then try this word again.",
                    actionLabel = "Try again",
                )
            }
        }

        composeRule.onNodeWithTag("status_offline").assertIsDisplayed()
        composeRule.onNodeWithText("You're offline").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertHasClickAction()
    }
}
