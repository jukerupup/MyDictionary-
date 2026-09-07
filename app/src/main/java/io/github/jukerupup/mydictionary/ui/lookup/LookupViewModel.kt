package io.github.jukerupup.mydictionary.ui.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.jukerupup.mydictionary.data.parser.WiktionaryAudio
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.lookup.LookupStateEngine
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.thesaurus.ThesaurusExpansionController
import io.github.jukerupup.mydictionary.domain.thesaurus.ThesaurusExpansionState
import io.github.jukerupup.mydictionary.ui.audio.PronunciationPlayback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LookupViewModel(
    repository: DictionaryRepository,
    audioController: AudioController,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = LookupStateEngine(repository = repository, scope = scope)
    private val playback = PronunciationPlayback(audioController = audioController, scope = scope)
    private val thesaurus = ThesaurusExpansionController(repository = repository, scope = scope)

    val state: StateFlow<LookupState> = engine.state
    val thesaurusState: StateFlow<ThesaurusExpansionState> = thesaurus.state

    init {
        scope.launch {
            playback.state.collect(engine::updateAudioState)
        }
    }

    fun lookup(query: String) {
        engine.lookup(query)
        thesaurus.setHeadword(query)
    }

    fun cancelLookup() = engine.cancel()

    fun toggleThesaurus() = thesaurus.toggle()

    fun retryThesaurus() = thesaurus.retry()

    fun requestPronunciation(audioReference: String) {
        playback.play(WiktionaryAudio.urlFor(audioReference))
    }

    override fun onCleared() {
        playback.release()
        thesaurus.close()
        scope.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: DictionaryRepository,
        private val audioController: AudioController,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LookupViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return LookupViewModel(repository, audioController) as T
        }
    }
}
