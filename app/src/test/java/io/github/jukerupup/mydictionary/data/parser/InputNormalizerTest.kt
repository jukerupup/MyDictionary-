package io.github.jukerupup.mydictionary.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputNormalizerTest {
    @Test
    fun trimsUnicodeWhitespaceAndSelectionEdgePunctuation() {
        assertEquals(
            NormalizationResult.Valid("state-of-the-art"),
            InputNormalizer.normalize("\u2003“state-of-the-art”\u00A0"),
        )
    }

    @Test
    fun preservesApostrophesHyphensAndPhrases() {
        assertEquals(
            NormalizationResult.Valid("don't give-up"),
            InputNormalizer.normalize("(don't give-up)"),
        )
    }

    @Test
    fun handlesUnicodeByCodePointRatherThanUtf16Unit() {
        val eightyCodePoints = "😀".repeat(40) + "a".repeat(40)
        assertEquals(NormalizationResult.Valid(eightyCodePoints), InputNormalizer.normalize(eightyCodePoints))
        assertEquals(
            NormalizationResult.Invalid(InputError.TooLong),
            InputNormalizer.normalize(eightyCodePoints + "b"),
        )
    }

    @Test
    fun rejectsNullBlankAndPunctuationOnlySelections() {
        listOf<CharSequence?>(null, "", "\u2003\u00A0", "…?!").forEach { input ->
            assertTrue(InputNormalizer.normalize(input) is NormalizationResult.Invalid)
            assertEquals(InputError.Blank, (InputNormalizer.normalize(input) as NormalizationResult.Invalid).error)
        }
    }
}
