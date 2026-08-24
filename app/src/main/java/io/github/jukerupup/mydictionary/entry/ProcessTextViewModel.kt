package io.github.jukerupup.mydictionary.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.lookup.LookupStateEngine
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

internal data class ProcessTextRequest(
    val text: CharSequence?,
    val readOnly: Boolean,
)

internal class ProcessTextViewModel(
    repository: DictionaryRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel() {
    private val ownedScope = scope
    private val engine = LookupStateEngine(repository, ownedScope, dispatcher)
    private var started = false

    val state: StateFlow<LookupState> = engine.state
    var request: ProcessTextRequest? = null
        private set

    fun start(request: ProcessTextRequest) {
        if (started) return
        started = true
        this.request = request
        engine.lookup(request.text)
    }

    fun dismiss() {
        engine.cancel()
    }

    fun requestPronunciation(audioReference: String) {
        if (audioReference.isNotBlank()) {
            engine.updateAudioState(
                AudioPlaybackState.Error("Pronunciation playback is not available yet."),
            )
        }
    }

    override fun onCleared() {
        ownedScope.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: DictionaryRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProcessTextViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ProcessTextViewModel(repository) as T
        }
    }
}
