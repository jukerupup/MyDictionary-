package io.github.jukerupup.mydictionary.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.jukerupup.mydictionary.data.parser.MerriamWebsterAudio
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.lookup.LookupStateEngine
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.ui.audio.PronunciationPlayback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal data class ProcessTextRequest(
    val text: CharSequence?,
    val readOnly: Boolean,
)

internal class ProcessTextViewModel(
    repository: DictionaryRepository,
    audioController: AudioController,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel() {
    private val ownedScope = scope
    private val engine = LookupStateEngine(repository, ownedScope, dispatcher)
    private val playback = PronunciationPlayback(audioController = audioController, scope = ownedScope)
    private var started = false

    val state: StateFlow<LookupState> = engine.state
    var request: ProcessTextRequest? = null
        private set

    init {
        ownedScope.launch {
            playback.state.collect(engine::updateAudioState)
        }
    }

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
        playback.play(MerriamWebsterAudio.urlFor(audioReference))
    }

    override fun onCleared() {
        playback.release()
        ownedScope.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: DictionaryRepository,
        private val audioController: AudioController,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProcessTextViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ProcessTextViewModel(repository, audioController) as T
        }
    }
}
