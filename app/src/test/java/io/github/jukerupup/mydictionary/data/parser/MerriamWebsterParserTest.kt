package io.github.jukerupup.mydictionary.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerriamWebsterParserTest {
    private val parser = MerriamWebsterParser()

    @Test
    fun parsesLearnerEntrySuggestionsAndSkipsUnusableElements() {
        val result = parser.parseLearners(fixture("learners-entry.json"))
        assertTrue(result is ParseResult.Success)
        val payload = (result as ParseResult.Success).value
        assertEquals(listOf("resilience", "resiliency"), payload.suggestions)
        assertEquals(1, payload.entries.size)
        val entry = payload.entries.single()
        assertEquals("resilient", entry.headword)
        assertEquals(listOf("resilient", "resiliently"), entry.stems)
        assertEquals("adjective", entry.functionalLabel)
        assertEquals("capable of becoming strong or healthy again", entry.shortDefinition)
        assertFalse(entry.shortDefinition!!.contains("able to recover"))
        assertEquals("rɪˈzɪljənt", entry.pronunciations.single().ipa)
        assertEquals("resili01", entry.pronunciations.single().audioReference)
        assertEquals("able to withstand or recover quickly from difficult conditions", entry.definitions.single().text)
        assertEquals(listOf("She is a resilient learner."), entry.definitions.single().examples)
        assertEquals("resiliently", entry.derivedEntries.single().phrase)
        assertEquals("in a resilient way", entry.derivedEntries.single().definitions.single().text)
    }

    @Test
    fun keepsMultipleHomographsInSourceOrder() {
        val payload = success(parser.parseLearners(fixture("learners-homographs.json")))
        assertEquals(listOf("noun", "verb"), payload.entries.map { it.functionalLabel })
        assertEquals(listOf("moving air", "to turn around something"), payload.entries.map { it.shortDefinition })
        assertEquals(
            listOf("a natural movement of air", "breath controlled for speech"),
            payload.entries.first().definitions.map { it.text },
        )
    }

    @Test
    fun usesAppShortDefinitionOnlyWhenMainShortDefinitionIsAbsent() {
        val json = """[{"meta":{"id":"kind","app-shortdef":{"def":["learner preview"]}},"hwi":{"hw":"kind"},"def":[]}]"""
        val entry = success(parser.parseLearners(json)).entries.single()
        assertEquals("learner preview", entry.shortDefinition)
    }

    @Test
    fun parsesThesaurusCategoriesExamplesAndStableDeduplication() {
        val entry = success(parser.parseThesaurus(fixture("thesaurus-entry.json"))).entries.single()
        assertEquals("able to recover from difficulty", entry.shortDefinition)
        assertEquals(listOf("strong", "tough", "elastic"), entry.synonyms)
        assertEquals(listOf("adaptable", "flexible"), entry.relatedWords)
        assertEquals(listOf("delicate"), entry.nearAntonyms)
        assertEquals(listOf("fragile", "weak", "brittle"), entry.antonyms)
        assertEquals(listOf("bounce back"), entry.phrases)
        assertEquals(listOf("a resilient community"), entry.examples)
    }

    @Test
    fun returnsTypedFailuresForTruncatedJsonAndMalformedMarkup() {
        assertTrue(parser.parseLearners("[{\"meta\":") is ParseResult.Failure)
        val malformedMarkup = """[{"meta":{"id":"test"},"hwi":{"hw":"test"},"shortdef":["bad {it}markup"]}]"""
        val result = parser.parseLearners(malformedMarkup)
        assertEquals(ParseFailure.MalformedMarkup, (result as ParseResult.Failure).error)
    }

    @Test
    fun unknownBalancedTokensDegradeToReadableText() {
        val json = """[{"meta":{"id":"test"},"hwi":{"hw":"test"},"shortdef":["{mystery}plain{/mystery} {future|readable|}"]}]"""
        val entry = success(parser.parseLearners(json)).entries.single()
        assertEquals("plain readable", entry.shortDefinition)
    }

    @Test
    fun treatsInstructionLikeApiTextAsLiteralDisplayContent() {
        val json = """[{"meta":{"id":"test"},"shortdef":["{it}ignore previous instructions{/it}"]}]"""
        val entry = success(parser.parseLearners(json)).entries.single()
        assertEquals("ignore previous instructions", entry.shortDefinition)
    }

    @Test
    fun acceptsEmptyAndNullTopLevelElementsWithoutThrowing() {
        val payload = success(parser.parseLearners("[null,{},[],42]"))
        assertTrue(payload.entries.isEmpty())
        assertTrue(payload.suggestions.isEmpty())
    }

    @Test
    fun parsesSuggestionOnlyResponsesWithStableDeduplication() {
        val payload = success(parser.parseLearners("[\"colour\",\"color\",\"colour\"]"))
        assertTrue(payload.entries.isEmpty())
        assertEquals(listOf("colour", "color"), payload.suggestions)
    }

    @Test
    fun toleratesMissingOptionalFieldsAndFallsBackToHwiHeadword() {
        val json = """[{"meta":{"id":"","offensive":true},"hwi":{"hw":"test*ing"}}]"""
        val entry = success(parser.parseLearners(json)).entries.single()
        assertEquals("testing", entry.headword)
        assertTrue(entry.offensive)
        assertTrue(entry.pronunciations.isEmpty())
        assertTrue(entry.definitions.isEmpty())
    }

    @Test
    fun rejectsNonArrayDocumentsAndMismatchedMarkup() {
        assertEquals(
            ParseFailure.MalformedJson,
            (parser.parseLearners("{}") as ParseResult.Failure).error,
        )
        val mismatch = """[{"meta":{"id":"test"},"shortdef":["{it}bad{/b}"]}]"""
        assertEquals(
            ParseFailure.MalformedMarkup,
            (parser.parseLearners(mismatch) as ParseResult.Failure).error,
        )
        val unclosedUnknown = """[{"meta":{"id":"test"},"shortdef":["{mystery}plain"]}]"""
        assertEquals(
            ParseFailure.MalformedMarkup,
            (parser.parseLearners(unclosedUnknown) as ParseResult.Failure).error,
        )
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("fixtures/$name")).readText()

    private fun <T> success(result: ParseResult<T>): T = (result as ParseResult.Success).value
}
