package io.github.jukerupup.mydictionary.data.parser

import io.github.jukerupup.mydictionary.domain.model.Definition
import io.github.jukerupup.mydictionary.domain.model.DerivedEntry
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.Pronunciation
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

class MerriamWebsterParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parseLearners(source: String): ParseResult<DictionaryPayload<DictionaryEntry>> =
        parsePayload(source, ::learnerEntry)

    fun parseThesaurus(source: String): ParseResult<DictionaryPayload<ThesaurusEntry>> =
        parsePayload(source, ::thesaurusEntry)

    private fun <T> parsePayload(
        source: String,
        entryParser: (JsonObject) -> T?,
    ): ParseResult<DictionaryPayload<T>> = try {
        val root = json.parseToJsonElement(source) as? JsonArray
            ?: return ParseResult.Failure(ParseFailure.MalformedJson)
        val entries = root.mapNotNull { (it as? JsonObject)?.let(entryParser) }
        val suggestions = root.mapNotNull { element ->
            (element as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
        }.distinct()
        ParseResult.Success(DictionaryPayload(entries, suggestions))
    } catch (_: MalformedMarkupException) {
        ParseResult.Failure(ParseFailure.MalformedMarkup)
    } catch (_: IllegalArgumentException) {
        ParseResult.Failure(ParseFailure.MalformedJson)
    }

    private fun learnerEntry(entry: JsonObject): DictionaryEntry? {
        val meta = entry.objectValue("meta")
        val hwi = entry.objectValue("hwi")
        val headword = headword(meta, hwi) ?: return null
        val shortDefinition = entry.renderedStrings("shortdef").firstOrNull()
            ?: meta?.objectValue("app-shortdef")?.renderedStrings("def")?.firstOrNull()
        return DictionaryEntry(
            headword = headword,
            functionalLabel = entry.stringValue("fl"),
            pronunciations = pronunciations(hwi),
            definitions = definitions(entry),
            offensive = meta?.get("offensive")?.primitive()?.booleanOrNull ?: false,
            stems = meta?.strings("stems").orEmpty().distinct(),
            shortDefinition = shortDefinition,
            derivedEntries = derivedEntries(entry),
        )
    }

    private fun thesaurusEntry(entry: JsonObject): ThesaurusEntry? {
        val meta = entry.objectValue("meta")
        val headword = headword(meta, entry.objectValue("hwi")) ?: return null
        val synonyms = linkedSetOf<String>().apply {
            addAll(meta.wordStrings("syns"))
            addAll(entry.wordsInNamedArrays("syn_list"))
        }
        val antonyms = linkedSetOf<String>().apply {
            addAll(meta.wordStrings("ants"))
            addAll(entry.wordsInNamedArrays("ant_list"))
        }
        val parsedDefinitions = definitions(entry)
        return ThesaurusEntry(
            headword = headword,
            functionalLabel = entry.stringValue("fl"),
            shortDefinition = entry.renderedStrings("shortdef").firstOrNull(),
            definitions = parsedDefinitions,
            synonyms = synonyms.toList(),
            relatedWords = entry.wordsInNamedArrays("rel_list"),
            nearAntonyms = entry.wordsInNamedArrays("near_list"),
            antonyms = antonyms.toList(),
            phrases = entry.wordsInNamedArrays("phrase_list"),
            examples = parsedDefinitions.flatMap(Definition::examples).distinct(),
        )
    }

    private fun headword(meta: JsonObject?, hwi: JsonObject?): String? {
        val metaHeadword = meta?.stringValue("id")?.substringBefore(':')?.takeIf(String::isNotBlank)
        return metaHeadword ?: hwi?.stringValue("hw")?.replace("*", "")?.takeIf(String::isNotBlank)
    }

    private fun pronunciations(hwi: JsonObject?): List<Pronunciation> =
        hwi?.arrayValue("prs").orEmpty().mapNotNull { element ->
            val pronunciation = element as? JsonObject ?: return@mapNotNull null
            val ipa = pronunciation.stringValue("ipa")
            val audio = pronunciation.objectValue("sound")?.stringValue("audio")
            if (ipa == null && audio == null) null else Pronunciation(ipa, audio)
        }.distinct()

    private fun definitions(container: JsonObject): List<Definition> = buildList {
        container["def"]?.walkObjects { candidate ->
            val dt = candidate.arrayValue("dt") ?: return@walkObjects
            parseDefinition(dt)?.let(::add)
        }
    }.distinct()

    private fun parseDefinition(dt: JsonArray): Definition? {
        val texts = mutableListOf<String>()
        val examples = mutableListOf<String>()
        dt.forEach { element ->
            val pair = element as? JsonArray ?: return@forEach
            when (pair.getOrNull(0)?.primitive()?.contentOrNull) {
                "text" -> pair.getOrNull(1)?.primitive()?.contentOrNull
                    ?.let(MerriamWebsterMarkup::render)
                    ?.takeIf(String::isNotBlank)
                    ?.let(texts::add)
                "vis" -> pair.getOrNull(1)?.walkObjects { example ->
                    example.stringValue("t")
                        ?.let(MerriamWebsterMarkup::render)
                        ?.takeIf(String::isNotBlank)
                        ?.let(examples::add)
                }
            }
        }
        val text = texts.joinToString(" ").takeIf(String::isNotBlank) ?: return null
        return Definition(text, examples.distinct())
    }

    private fun derivedEntries(entry: JsonObject): List<DerivedEntry> =
        entry.arrayValue("dros").orEmpty().mapNotNull { element ->
            val derived = element as? JsonObject ?: return@mapNotNull null
            val phrase = derived.stringValue("drp")?.let(MerriamWebsterMarkup::render)
                ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val definitions = definitions(derived)
            DerivedEntry(phrase, definitions)
        }
}

private fun JsonElement.primitive(): JsonPrimitive? = this as? JsonPrimitive

private fun JsonObject.objectValue(name: String): JsonObject? = get(name) as? JsonObject

private fun JsonObject.arrayValue(name: String): JsonArray? = get(name) as? JsonArray

private fun JsonObject.stringValue(name: String): String? =
    get(name)?.primitive()?.takeIf { it.isString }?.contentOrNull

private fun JsonObject.strings(name: String): List<String> =
    arrayValue(name).orEmpty().mapNotNull { it.primitive()?.takeIf(JsonPrimitive::isString)?.contentOrNull }

private fun JsonObject.renderedStrings(name: String): List<String> =
    strings(name).map(MerriamWebsterMarkup::render).filter(String::isNotBlank)

private fun JsonObject?.wordStrings(name: String): List<String> {
    if (this == null) return emptyList()
    val values = mutableListOf<String>()
    get(name)?.walk { element ->
        element.primitive()?.takeIf(JsonPrimitive::isString)?.contentOrNull?.let(values::add)
    }
    return values.distinct()
}

private fun JsonObject.wordsInNamedArrays(name: String): List<String> {
    val values = linkedSetOf<String>()
    walkObjects { candidate ->
        candidate[name]?.walkObjects { word ->
            word.stringValue("wd")
                ?.let(MerriamWebsterMarkup::render)
                ?.takeIf(String::isNotBlank)
                ?.let(values::add)
        }
    }
    return values.toList()
}

private fun JsonElement.walk(visit: (JsonElement) -> Unit) {
    visit(this)
    when (this) {
        is JsonArray -> forEach { it.walk(visit) }
        is JsonObject -> values.forEach { it.walk(visit) }
        else -> Unit
    }
}

private fun JsonElement.walkObjects(visit: (JsonObject) -> Unit) {
    walk { element -> if (element is JsonObject) visit(element) }
}
