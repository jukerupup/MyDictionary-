package io.github.jukerupup.mydictionary.data.parser

import io.github.jukerupup.mydictionary.domain.model.Definition
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.Pronunciation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses Wiktionary responses into domain models.
 *
 * English definitions come from the English Wiktionary REST API (clean JSON).
 * Chinese translations come from the Chinese Wiktionary wikitext, whose
 * definition lines look like `# 彈回的` / `# 有彈力的`.
 */
class WiktionaryParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parseEnglish(source: String): ParseResult<DictionaryPayload<DictionaryEntry>> = try {
        val root = json.parseToJsonElement(source).jsonObject
        val entries = root["en"]?.jsonArray.orEmpty()
            .mapNotNull { (it as? JsonObject)?.parseEnglishEntry() }
        ParseResult.Success(DictionaryPayload(entries = entries, suggestions = emptyList()))
    } catch (_: IllegalArgumentException) {
        ParseResult.Failure(ParseFailure.MalformedJson)
    }

    fun parseChinese(source: String): ChinesePayload = try {
        val root = json.parseToJsonElement(source).jsonObject
        val wikitext = root["parse"]?.jsonObject
            ?.get("wikitext")?.jsonObject
            ?.get("*")?.jsonPrimitive?.contentOrNull
            ?: return ChinesePayload(emptyList(), null)
        ChinesePayload(
            translations = wikitext.extractChineseTranslations(),
            audioReference = wikitext.extractAudioReference(),
        )
    } catch (_: IllegalArgumentException) {
        ChinesePayload(emptyList(), null)
    }

    /** Extracts usage examples (`#* {{quote-text|...|passage=...}}` and `#: ...`) from en wikitext. */
    fun parseEnglishExamples(source: String): List<String> = try {
        val root = json.parseToJsonElement(source).jsonObject
        val wikitext = root["parse"]?.jsonObject
            ?.get("wikitext")?.jsonObject
            ?.get("*")?.jsonPrimitive?.contentOrNull
            ?: return emptyList()
        wikitext.extractExamples()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    private fun JsonObject.parseEnglishEntry(): DictionaryEntry? {
        val language = this["language"]?.jsonPrimitive?.contentOrNull
        if (language != "English") return null
        val partOfSpeech = this["partOfSpeech"]?.jsonPrimitive?.contentOrNull
        val definitions = this["definitions"]?.jsonArray.orEmpty()
            .mapNotNull { it.definitionText() }
            .filter { it.isNotBlank() }
            .map { Definition(it) }
        if (definitions.isEmpty()) return null

        // The REST API definition endpoint does not carry IPA/audio; fill
        // pronunciation from a separate pronunciations response (see repository).
        return DictionaryEntry(
            headword = "",
            functionalLabel = partOfSpeech,
            pronunciations = emptyList(),
            definitions = definitions,
            offensive = false,
        )
    }

    private fun kotlinx.serialization.json.JsonElement.definitionText(): String? {
        val obj = this as? JsonObject ?: return null
        val text = obj["definition"]?.jsonPrimitive?.contentOrNull ?: return null
        return text.stripMarkup()
    }
}

/** Removes `<a href=...>link</a>`, `<ol>`, `<span>`, and other tags, keeping text. */
private fun String.stripMarkup(): String = this
    .replace(Regex("<[^>]+>"), "")
    .replace(Regex("\\s+"), " ")
    .trim()

/**
 * Extracts Chinese translations from Chinese Wiktionary wikitext.
 *
 * Wikitext definition lines look like:
 * ```
 * ==英語==
 * resilient
 * # 彈回的
 * # 有彈力的
 * # 愉快的
 * ```
 */
private fun String.extractChineseTranslations(): List<String> {
    val result = mutableListOf<String>()
    for (line in lineSequence()) {
        val trimmed = line.trim()
        if (!trimmed.startsWith("#")) continue
        // Strip any remaining wiki markup, links, and templates.
        val cleaned = trimmed
            .removePrefix("#")
            .replace(Regex("\\[\\[(?:[^|\\]]*\\|)?([^\\]]+)\\]\\]"), "$1")
            .replace(Regex("\\{\\{[^}]*\\}\\}"), "")
            .trim()
        if (cleaned.isNotEmpty() && cleaned.any { char -> CJK_RANGES.any { char.code in it } }) {
            result.add(cleaned)
        }
    }
    return result.distinct()
}

private val CJK_RANGES = listOf(
    0x4E00..0x9FFF,     // CJK Unified Ideographs
    0x3400..0x4DBF,     // CJK Extension A
    0xF900..0xFAFF,     // CJK Compatibility Ideographs
)

/** Extracts the first English pronunciation audio filename (e.g. `en-us-resilient.ogg`). */
private fun String.extractAudioReference(): String? {
    val match = Regex("""\{\{audio\|en\|([^}|]+)""").find(this) ?: return null
    return match.groupValues[1].trim()
}

/**
 * Extracts usage examples from English Wiktionary wikitext.
 *
 * Examples appear in two shapes:
 * - `#* {{quote-text|en|...|passage=He's resilient.}}`
 * - `#: an example sentence`
 */
private fun String.extractExamples(): List<String> {
    val result = mutableListOf<String>()
    for (line in lineSequence()) {
        val trimmed = line.trim()
        val example = when {
            trimmed.startsWith("#*") -> {
                // quote-text template: pull the passage= argument.
                val passage = Regex("""passage=([^|}]+)""").find(trimmed)?.groupValues?.get(1)
                    ?: continue
                passage.stripMarkup()
            }
            trimmed.startsWith("#:") -> trimmed.removePrefix("#:").stripMarkup()
            else -> continue
        }
        if (example.isNotEmpty() && example.length > 4) result.add(example)
    }
    return result.distinct().take(MAX_EXAMPLES)
}

private const val MAX_EXAMPLES = 6

data class ChinesePayload(
    val translations: List<String>,
    val audioReference: String?,
)
