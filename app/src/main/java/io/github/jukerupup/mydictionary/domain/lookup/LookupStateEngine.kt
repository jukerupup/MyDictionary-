package io.github.jukerupup.mydictionary.domain.lookup

import io.github.jukerupup.mydictionary.data.parser.InputError
import io.github.jukerupup.mydictionary.data.parser.InputNormalizer
import io.github.jukerupup.mydictionary.data.parser.NormalizationResult
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ThesaurusLoadState {
    data object NotRequested : ThesaurusLoadState
    data object Loading : ThesaurusLoadState
    data class Loaded(val entries: List<ThesaurusEntry>) : ThesaurusLoadState
    data class Suggestions(val values: List<String>) : ThesaurusLoadState
    data object NoMatch : ThesaurusLoadState
    data class Failure(val error: DictionaryError) : ThesaurusLoadState
}

sealed interface LookupState {
    data object Idle : LookupState
    data class Loading(val query: String) : LookupState
    data class Content(
        val query: String,
        val entries: List<DictionaryEntry>,
        val thesaurus: ThesaurusLoadState = ThesaurusLoadState.NotRequested,
        val audio: AudioPlaybackState = AudioPlaybackState.Idle,
    ) : LookupState
    data class Suggestions(val query: String, val values: List<String>) : LookupState
    data class NoMatch(val query: String) : LookupState
    data class InvalidInput(val error: InputError) : LookupState
    data class Failure(val query: String, val error: DictionaryError) : LookupState
}

class LookupStateEngine(
    private val repository: DictionaryRepository,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableState = MutableStateFlow<LookupState>(LookupState.Idle)
    val state: StateFlow<LookupState> = mutableState.asStateFlow()

    private val generation = AtomicLong()
    private var definitionJob: Job? = null
    private var thesaurusJob: Job? = null

    fun lookup(rawQuery: CharSequence?) {
        val normalized = InputNormalizer.normalize(rawQuery)
        if (normalized is NormalizationResult.Invalid) {
            cancelRequests()
            generation.incrementAndGet()
            mutableState.value = LookupState.InvalidInput(normalized.error)
            return
        }
        val query = (normalized as NormalizationResult.Valid).value
        if (definitionJob?.isActive == true && (state.value as? LookupState.Loading)?.query == query) {
            return
        }

        cancelRequests()
        val requestGeneration = generation.incrementAndGet()
        mutableState.value = LookupState.Loading(query)
        definitionJob = scope.launch {
            val result = withContext(dispatcher) { repository.lookupDefinition(query) }
            if (requestGeneration == generation.get()) mutableState.value = result.toLookupState(query)
        }
    }

    fun loadThesaurus() {
        val content = state.value as? LookupState.Content ?: return
        if (content.thesaurus is ThesaurusLoadState.Loading ||
            content.thesaurus is ThesaurusLoadState.Loaded
        ) return

        val requestGeneration = generation.get()
        mutableState.value = content.copy(thesaurus = ThesaurusLoadState.Loading)
        thesaurusJob = scope.launch {
            val result = withContext(dispatcher) { repository.lookupThesaurus(content.query) }
            val current = state.value as? LookupState.Content
            if (requestGeneration == generation.get() && current?.query == content.query) {
                mutableState.value = current.copy(thesaurus = result.toThesaurusState())
            }
        }
    }

    fun updateAudioState(audio: AudioPlaybackState) {
        val content = state.value as? LookupState.Content ?: return
        mutableState.value = content.copy(audio = audio)
    }

    fun cancel() {
        cancelRequests()
        generation.incrementAndGet()
        mutableState.value = LookupState.Idle
    }

    private fun cancelRequests() {
        definitionJob?.cancel()
        thesaurusJob?.cancel()
        definitionJob = null
        thesaurusJob = null
    }
}

private fun LookupResult<List<DictionaryEntry>>.toLookupState(query: String): LookupState = when (this) {
    is LookupResult.Success -> LookupState.Content(query, value)
    is LookupResult.Suggestions -> LookupState.Suggestions(query, values)
    LookupResult.NoMatch -> LookupState.NoMatch(query)
    is LookupResult.Failure -> LookupState.Failure(query, error)
}

private fun LookupResult<List<ThesaurusEntry>>.toThesaurusState(): ThesaurusLoadState = when (this) {
    is LookupResult.Success -> ThesaurusLoadState.Loaded(value)
    is LookupResult.Suggestions -> ThesaurusLoadState.Suggestions(values)
    LookupResult.NoMatch -> ThesaurusLoadState.NoMatch
    is LookupResult.Failure -> ThesaurusLoadState.Failure(error)
}
