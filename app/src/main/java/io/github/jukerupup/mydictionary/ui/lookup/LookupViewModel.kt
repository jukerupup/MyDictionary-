package io.github.jukerupup.mydictionary.ui.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.lookup.LookupStateEngine
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

class LookupViewModel(
    repository: DictionaryRepository,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = LookupStateEngine(repository = repository, scope = scope)

    val state: StateFlow<LookupState> = engine.state

    fun lookup(query: String) = engine.lookup(query)

    fun cancelLookup() = engine.cancel()

    fun requestPronunciation(audioReference: String) {
        if (audioReference.isNotBlank()) {
            engine.updateAudioState(
                AudioPlaybackState.Error("Pronunciation playback is not available yet."),
            )
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: DictionaryRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LookupViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return LookupViewModel(repository) as T
        }
    }
}
