package io.github.jukerupup.mydictionary.ui.showcase

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrimitiveShowcaseTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun coreLearnerPrimitivesExposeActionsAndStates() {
        composeRule.setContent {
            MyDictionaryTheme {
                PrimitiveShowcaseScreen()
            }
        }

        composeRule.onAllNodesWithTag("search_input").onFirst().assertIsDisplayed().assertIsEnabled()
        composeRule.onAllNodesWithTag("word_header").onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag("pronunciation_control").assertHasClickAction()
        composeRule.onNodeWithTag("definition_block").assertIsDisplayed()
        composeRule.onNodeWithTag("showcase_list").performScrollToNode(hasTestTag("example_block"))
        composeRule.onNodeWithTag("example_block").assertIsDisplayed()
        composeRule.onNodeWithTag("showcase_list").performScrollToNode(hasTestTag("open_quick_define_dialog"))
        composeRule.onNodeWithTag("open_quick_define_dialog").performClick()
        composeRule.onNodeWithTag("quick_define_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()

        composeRule.onNodeWithTag("showcase_list").performScrollToNode(hasTestTag("status_error"))
        composeRule.onNodeWithTag("status_error").assertIsDisplayed()
        composeRule.onNodeWithTag("showcase_list").performScrollToNode(hasTestTag("showcase_expansion"))
        composeRule.onAllNodesWithTag("expansion_row").onFirst().performClick()
        composeRule.onNodeWithTag("showcase_list").performScrollToNode(hasTestTag("showcase_dialog"))
        composeRule.onAllNodesWithTag("quick_define_card").onFirst().assertIsDisplayed()
    }

    @Test
    fun darkThemeAtTwoHundredPercentFontKeepsRecoveryContentReachable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = 2f,
                ),
            ) {
                MyDictionaryTheme(darkTheme = true) {
                    PrimitiveShowcaseScreen()
                }
            }
        }

        composeRule.onNodeWithTag("primitive_showcase").assertIsDisplayed()
        composeRule.onNodeWithTag("showcase_list").performScrollToNode(hasTestTag("status_offline"))
        composeRule.onNodeWithTag("status_offline").assertIsDisplayed()
        composeRule.onNodeWithTag("showcase_list").performScrollToNode(hasTestTag("showcase_dialog"))
        composeRule.onNodeWithTag("showcase_dialog").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Pronunciation audio is not available for counterrevolutionaries",
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("showcase_list").performScrollToNode(hasTestTag("open_missing_audio_dialog"))
        composeRule.onNodeWithTag("open_missing_audio_dialog").performClick()
        composeRule.onNodeWithTag("quick_define_dialog").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Pronunciation audio is not available for counterrevolutionaries",
        ).assertIsDisplayed()
    }
}
