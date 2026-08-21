package io.github.jukerupup.mydictionary.domain.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioController {
    val state: StateFlow<AudioPlaybackState>

    fun play(url: String)
    fun stop()
    fun release()
}

sealed interface AudioPlaybackState {
    data object Idle : AudioPlaybackState
    data object Loading : AudioPlaybackState
    data object Playing : AudioPlaybackState
    data object Completed : AudioPlaybackState
    data class Error(val message: String) : AudioPlaybackState
}
