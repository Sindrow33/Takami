package dev.anime.player.core

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Media3Engine(context: Context) : PlayerEngine {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state

    private val handler = Handler(Looper.getMainLooper())

    private val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 13) AnimePlayer/0.1")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)

    private val exo: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) = push()
                override fun onIsPlayingChanged(isPlaying: Boolean) = push()
                override fun onPlayerError(error: PlaybackException) {
                    _state.value = _state.value.copy(error = error.errorCodeName)
                }
            })
        }

    private val ticker = object : Runnable {
        override fun run() {
            push()
            handler.postDelayed(this, 200L)
        }
    }

    init { handler.post(ticker) }

    private fun push() {
        _state.value = _state.value.copy(
            isPlaying = exo.isPlaying,
            positionMs = exo.currentPosition.coerceAtLeast(0L),
            durationMs = exo.duration.let { if (it > 0L) it else 0L },
            bufferedMs = exo.bufferedPosition.coerceAtLeast(0L),
            isBuffering = exo.playbackState == Player.STATE_BUFFERING,
        )
    }

    override fun load(url: String, headers: Map<String, String>, startMs: Long) {
        if (headers.isNotEmpty()) httpFactory.setDefaultRequestProperties(headers)
        _state.value = _state.value.copy(error = null)
        exo.setMediaItem(MediaItem.fromUri(url), startMs)
        exo.prepare()
        exo.playWhenReady = true
    }

    override fun play() { exo.play() }

    override fun pause() { exo.pause() }

    override fun seekTo(ms: Long) {
        val dur = exo.duration
        exo.seekTo(if (dur > 0L) ms.coerceIn(0L, dur) else ms.coerceAtLeast(0L))
    }

    override fun setSpeed(speed: Float) { exo.setPlaybackSpeed(speed) }

    override fun setVolume(volume: Float) { exo.volume = volume.coerceIn(0f, 1f) }

    override fun addSubtitleTrack(url: String, language: String, mimeType: String) {
        val current = exo.currentMediaItem ?: return
        val sub = MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
            .setMimeType(if (mimeType.isBlank()) MimeTypes.TEXT_VTT else mimeType)
            .setLanguage(language)
            .build()
        val pos = exo.currentPosition
        val existing = current.localConfiguration?.subtitleConfigurations ?: emptyList()
        exo.setMediaItem(
            current.buildUpon().setSubtitleConfigurations(existing + sub).build(),
            pos,
        )
        exo.prepare()
    }

    override fun release() {
        handler.removeCallbacksAndMessages(null)
        exo.release()
    }

    /** Только для привязки PlayerView. */
    fun exoInstance(): ExoPlayer = exo
}
