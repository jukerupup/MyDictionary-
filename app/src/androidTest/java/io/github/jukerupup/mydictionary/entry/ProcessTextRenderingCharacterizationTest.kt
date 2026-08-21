package io.github.jukerupup.mydictionary.entry

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jukerupup.mydictionary.ui.components.QuickDefineCard
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextRenderingCharacterizationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun approvedLearnerResultRenderingKeepsMeaningExampleAudioAndClose() {
        composeRule.setContent {
            MyDictionaryTheme {
                QuickDefineCard(
                    word = "resilient",
                    definition = "able to become strong after difficulty",
                    example = "She remained resilient after the setback.",
                    audioAvailable = true,
                )
            }
        }

        composeRule.onNodeWithTag("quick_define_card").assertIsDisplayed()
        composeRule.onNodeWithText("resilient").assertIsDisplayed()
        composeRule.onNodeWithText("able to become strong after difficulty").assertIsDisplayed()
        composeRule.onNodeWithText("She remained resilient after the setback.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Listen for resilient").assertHasClickAction()
        composeRule.onNodeWithText("Close").assertHasClickAction()
    }
}
