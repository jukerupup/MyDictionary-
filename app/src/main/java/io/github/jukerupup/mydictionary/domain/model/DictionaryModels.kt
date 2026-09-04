package io.github.jukerupup.mydictionary.domain.model

data class DictionaryEntry(
    val headword: String,
    val functionalLabel: String?,
    val pronunciations: List<Pronunciation>,
    val definitions: List<Definition>,
    val offensive: Boolean,
    val stems: List<String> = emptyList(),
    val shortDefinition: String? = null,
    val derivedEntries: List<DerivedEntry> = emptyList(),
    val translations: List<String> = emptyList(),
)

data class Pronunciation(
    val ipa: String?,
    val audioReference: String?,
)

data class Definition(
    val text: String,
    val examples: List<String> = emptyList(),
)

data class DerivedEntry(
    val phrase: String,
    val definitions: List<Definition>,
)

data class ThesaurusEntry(
    val headword: String,
    val functionalLabel: String? = null,
    val shortDefinition: String? = null,
    val definitions: List<Definition> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val relatedWords: List<String> = emptyList(),
    val nearAntonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val phrases: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
)

enum class DictionaryCredential {
    Learners,
    Thesaurus,
}
