package io.github.jukerupup.mydictionary.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Media3AudioController internal constructor(
    private val engine: PronunciationPlayerEngine,
) : AudioController {
    constructor(context: Context) : this(Media3PronunciationPlayerEngine(context))

    private val mutableState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    override val state: StateFlow<AudioPlaybackState> = mutableState.asStateFlow()

    private var requestId = 0L
    private var activeRequestId: Long? = null
    private var released = false

    init {
        engine.setListener { eventRequestId, event ->
            if (released || eventRequestId != activeRequestId) return@setListener
            mutableState.value = when (event) {
                PlayerEvent.Loading -> AudioPlaybackState.Loading
                PlayerEvent.Playing -> AudioPlaybackState.Playing
                PlayerEvent.Completed -> AudioPlaybackState.Completed
                PlayerEvent.Error -> AudioPlaybackState.Error(
                    "Pronunciation audio could not be played",
                )
            }
        }
    }

    override fun play(url: String) {
        if (released) return
        val nextRequestId = ++requestId
        activeRequestId = nextRequestId
        mutableState.value = AudioPlaybackState.Loading
        engine.play(url = url, requestId = nextRequestId)
    }

    override fun stop() {
        if (released) return
        activeRequestId = null
        engine.stop()
        mutableState.value = AudioPlaybackState.Idle
    }

    override fun release() {
        if (released) return
        released = true
        activeRequestId = null
        engine.release()
        mutableState.value = AudioPlaybackState.Idle
    }
}

internal enum class PlayerEvent {
    Loading,
    Playing,
    Completed,
    Error,
}

internal interface PronunciationPlayerEngine {
    fun setListener(listener: (requestId: Long, event: PlayerEvent) -> Unit)
    fun play(url: String, requestId: Long)
    fun stop()
    fun release()
}

private class Media3PronunciationPlayerEngine(context: Context) : PronunciationPlayerEngine {
    private val player = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true,
        )
        .build()
    private var listener: (Long, PlayerEvent) -> Unit = { _, _ -> }

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val currentRequestId = player.currentRequestId ?: return
                    when (playbackState) {
                        Player.STATE_IDLE -> Unit
                        Player.STATE_BUFFERING -> listener(currentRequestId, PlayerEvent.Loading)
                        Player.STATE_READY -> if (player.playWhenReady) {
                            listener(currentRequestId, PlayerEvent.Playing)
                        }
                        Player.STATE_ENDED -> {
                            player.playWhenReady = false
                            listener(currentRequestId, PlayerEvent.Completed)
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        player.currentRequestId?.let { listener(it, PlayerEvent.Playing) }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    player.currentRequestId?.let { listener(it, PlayerEvent.Error) }
                }
            },
        )
    }

    override fun setListener(listener: (requestId: Long, event: PlayerEvent) -> Unit) {
        this.listener = listener
    }

    override fun play(url: String, requestId: Long) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(
            MediaItem.Builder()
                .setMediaId(requestId.toString())
                .setUri(url)
                .build(),
        )
        player.prepare()
        player.playWhenReady = true
    }

    override fun stop() {
        player.stop()
        player.clearMediaItems()
    }

    override fun release() {
        player.release()
    }

    private val Player.currentRequestId: Long?
        get() = currentMediaItem?.mediaId?.toLongOrNull()
}
