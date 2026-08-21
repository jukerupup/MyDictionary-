package io.github.jukerupup.mydictionary.ui.lookup

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jukerupup.mydictionary.ui.components.StatusKind
import io.github.jukerupup.mydictionary.ui.components.StatusPanel
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
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
