package io.github.jukerupup.mydictionary.domain.thesaurus

import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ThesaurusExpansionState(
    val headword: String = "",
    val expanded: Boolean = false,
    val content: ThesaurusLoadState = ThesaurusLoadState.Idle,
)

sealed interface ThesaurusLoadState {
    data object Idle : ThesaurusLoadState
    data object Loading : ThesaurusLoadState
    data class Ready(val value: ThesaurusContent) : ThesaurusLoadState
    data class Error(val title: String, val message: String) : ThesaurusLoadState
}

data class ThesaurusContent(
    val meaning: String? = null,
    val synonyms: List<String> = emptyList(),
    val relatedWords: List<String> = emptyList(),
    val nearAntonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val phrases: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
)

class ThesaurusExpansionController(
    private val repository: DictionaryRepository,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : AutoCloseable {
    private val mutableState = MutableStateFlow(ThesaurusExpansionState())
    val state: StateFlow<ThesaurusExpansionState> = mutableState.asStateFlow()
    private var request: Job? = null
    private var generation = 0L

    fun setHeadword(headword: String) {
        val currentWord = headword.trim()
        if (currentWord == mutableState.value.headword) return
        generation += 1
        request?.cancel()
        request = null
        mutableState.value = ThesaurusExpansionState(headword = currentWord)
    }

    fun toggle() {
        val current = mutableState.value
        val expanded = !current.expanded
        mutableState.value = current.copy(expanded = expanded)
        if (expanded && current.content == ThesaurusLoadState.Idle && current.headword.isNotEmpty()) {
            load(current.headword)
        }
    }

    fun retry() {
        val current = mutableState.value
        if (current.expanded && current.content is ThesaurusLoadState.Error && current.headword.isNotEmpty()) {
            load(current.headword)
        }
    }

    override fun close() {
        generation += 1
        request?.cancel()
        request = null
    }

    private fun load(headword: String) {
        if (request?.isActive == true) return
        val requestGeneration = ++generation
        mutableState.value = mutableState.value.copy(content = ThesaurusLoadState.Loading)
        request = scope.launch(dispatcher) {
            val result = repository.lookupThesaurus(headword)
            if (requestGeneration != generation || mutableState.value.headword != headword) return@launch
            mutableState.value = mutableState.value.copy(content = result.toLoadState())
            request = null
        }
    }
}

private fun LookupResult<List<ThesaurusEntry>>.toLoadState(): ThesaurusLoadState = when (this) {
    is LookupResult.Success -> {
        val content = value.toContent()
        if (content == ThesaurusContent()) {
            ThesaurusLoadState.Error("No thesaurus match", "No related vocabulary was found for this word.")
        } else {
            ThesaurusLoadState.Ready(content)
        }
    }
    is LookupResult.Suggestions -> ThesaurusLoadState.Error(
        "No thesaurus match",
        values.firstOrNull()?.let { "Try a nearby spelling such as $it." }
            ?: "No related vocabulary was found for this word.",
    )
    LookupResult.NoMatch -> ThesaurusLoadState.Error(
        "No thesaurus match",
        "No related vocabulary was found for this word.",
    )
    is LookupResult.Failure -> error.toLoadState()
}

private fun DictionaryError.toLoadState(): ThesaurusLoadState.Error = when (this) {
    is DictionaryError.MissingConfiguration -> {
        val missingThesaurus = DictionaryCredential.Thesaurus in credentials
        ThesaurusLoadState.Error(
            "Thesaurus unavailable",
            if (missingThesaurus) "Add a thesaurus API key to use related vocabulary."
            else "Thesaurus configuration is incomplete.",
        )
    }
    DictionaryError.InvalidCredential -> ThesaurusLoadState.Error(
        "Thesaurus unavailable",
        "The thesaurus API key was not accepted.",
    )
    DictionaryError.QuotaExceeded -> ThesaurusLoadState.Error(
        "Thesaurus limit reached",
        "The thesaurus service limit has been reached. Try again later.",
    )
    DictionaryError.Offline -> ThesaurusLoadState.Error(
        "Thesaurus offline",
        "Check your connection, then try again.",
    )
    DictionaryError.Timeout -> ThesaurusLoadState.Error(
        "Thesaurus timed out",
        "The related-vocabulary request took too long. Try again.",
    )
    DictionaryError.NonJsonResponse,
    is DictionaryError.MalformedContent,
    is DictionaryError.Server,
    -> ThesaurusLoadState.Error(
        "Thesaurus could not load",
        "Related vocabulary is temporarily unavailable. Try again.",
    )
}

private fun List<ThesaurusEntry>.toContent(): ThesaurusContent = ThesaurusContent(
    meaning = asSequence()
        .mapNotNull { entry ->
            entry.shortDefinition.cleanOrNull()
                ?: entry.definitions.asSequence().map { it.text }.firstNotNullOfOrNull(String::cleanOrNull)
        }
        .firstOrNull(),
    synonyms = flatMap { it.synonyms }.stableClean(),
    relatedWords = flatMap { it.relatedWords }.stableClean(),
    nearAntonyms = flatMap { it.nearAntonyms }.stableClean(),
    antonyms = flatMap { it.antonyms }.stableClean(),
    phrases = flatMap { it.phrases }.stableClean(),
    examples = flatMap { entry -> entry.examples + entry.definitions.flatMap { it.examples } }.stableClean(),
)

private fun List<String>.stableClean(): List<String> = buildList {
    val seen = mutableSetOf<String>()
    this@stableClean.forEach { raw ->
        val value = raw.trim()
        if (value.isNotEmpty() && seen.add(value)) add(value)
    }
}

private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
