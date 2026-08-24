package io.github.jukerupup.mydictionary.entry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import io.github.jukerupup.mydictionary.ui.components.QuickDefineCard
import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.model.Definition
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.ui.theme.DictionaryLayout
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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

    @Test
    fun twoHundredPercentScrollKeepsTitleAndCloseVisible() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MyDictionaryTheme {
                    QuickDefineCard(
                        state = LookupState.Content(
                            "resilient",
                            listOf(
                                DictionaryEntry(
                                    headword = "resilient",
                                    functionalLabel = "adjective",
                                    pronunciations = emptyList(),
                                    definitions = listOf(
                                        Definition("long learner definition ".repeat(20)),
                                    ),
                                    offensive = false,
                                ),
                            ),
                        ),
                        selectedText = "resilient",
                        onDismiss = {},
                        modifier = Modifier.heightIn(max = 320.dp),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("quick_define_card").performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Quick Define").assertIsDisplayed()
        composeRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun dialogMinimumWidthMatchesTheDesignContract() {
        check(DictionaryLayout.MinDialogWidth == 280.dp)
    }
}
