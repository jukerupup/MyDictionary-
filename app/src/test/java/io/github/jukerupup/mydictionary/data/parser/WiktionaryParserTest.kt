package io.github.jukerupup.mydictionary.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WiktionaryParserTest {
    private val parser = WiktionaryParser()

    @Test
    fun `parses english definitions from REST api json`() {
        val source = """
            {
              "en": [
                {
                  "partOfSpeech": "Adjective",
                  "language": "English",
                  "definitions": [
                    {
                      "definition": "Returning quickly to original shape after force is applied; <a href=\"/wiki/elastic\">elastic</a>."
                    },
                    {
                      "definition": "Having the ability to recover from mental illness, trauma, etc.; having resilience."
                    }
                  ]
                },
                {
                  "partOfSpeech": "Verb",
                  "language": "Latin",
                  "definitions": [
                    { "definition": "third-person plural future active indicative of resilio" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = parser.parseEnglish(source)

        assertTrue(result is ParseResult.Success)
        val payload = (result as ParseResult.Success).value
        assertEquals(1, payload.entries.size)
        val entry = payload.entries.single()
        assertEquals("Adjective", entry.functionalLabel)
        assertEquals(
            "Returning quickly to original shape after force is applied; elastic.",
            entry.definitions.first().text,
        )
    }

    @Test
    fun `strips html markup from definitions`() {
        val source = """
            {"en":[{"partOfSpeech":"Adjective","language":"English","definitions":[
              {"definition":"<span class=\"x\">Having the ability</span> to absorb energy when deformed."}
            ]}]}
        """.trimIndent()

        val result = parser.parseEnglish(source)
        val entry = (result as ParseResult.Success).value.entries.single()
        assertEquals("Having the ability to absorb energy when deformed.", entry.definitions.first().text)
    }

    @Test
    fun `extracts chinese translations and audio from zh wikitext`() {
        val source = """
            {
              "parse": {
                "title": "resilient",
                "wikitext": { "*": "==英語==\n\n===發音===\n* {{audio|en|en-us-resilient.ogg|音頻（美式）}}\n\n:'''resilient'''\n#[[彈回的]]\n#[[有彈力的]]\n#[[愉快的]]" }
              }
            }
        """.trimIndent()

        val payload = parser.parseChinese(source)

        assertEquals(listOf("彈回的", "有彈力的", "愉快的"), payload.translations)
        assertEquals("en-us-resilient.ogg", payload.audioReference)
    }

    @Test
    fun `returns empty translations when zh page has no chinese`() {
        val source = """
            {"parse":{"title":"resilient","wikitext":{"*":"==English==\n===Noun===\n# definition"}}}
        """.trimIndent()

        val payload = parser.parseChinese(source)

        assertEquals(emptyList<String>(), payload.translations)
        assertNotNull(payload)
    }

    @Test
    fun `malformed english json is a typed failure`() {
        val result = parser.parseEnglish("not json at all")
        assertTrue(result is ParseResult.Failure)
    }
}
