package dev.anime.player.core

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.anime.player.track.MediaTrack
import dev.anime.player.track.TrackKind
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

    /**
     * Важно: фабрика именно [DefaultDataSource.Factory], а не сама HTTP-фабрика.
     * С одной HTTP-фабрикой движок умел только `http(s)://` — локальный файл и
     * документ из выбранной пользователем папки (`file://`, `content://`)
     * не открывались вообще. `DefaultDataSource` выбирает источник по схеме и
     * для сети использует переданную HTTP-фабрику, так что заголовки и таймауты
     * для потоков сохраняются.
     */
    private val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

    private val exo: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
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

    // --- Дорожки звука и субтитров ---------------------------------------

    /**
     * Дорожки текущего файла в модели [MediaTrack].
     *
     * Своя модель, а не `Tracks.Group`, потому что вся логика выбора должна
     * быть проверяема JVM-тестом; здесь только перевод из Media3 и обратно
     * по паре индексов.
     */
    fun availableTracks(): List<MediaTrack> {
        val out = mutableListOf<MediaTrack>()
        val groups = exo.currentTracks.groups
        groups.forEachIndexed { groupIndex, group ->
            val kind = when (group.type) {
                C.TRACK_TYPE_AUDIO -> TrackKind.Audio
                C.TRACK_TYPE_TEXT -> TrackKind.Subtitle
                else -> null
            } ?: return@forEachIndexed
            for (formatIndex in 0 until group.length) {
                if (!group.isTrackSupported(formatIndex)) continue
                val format = group.getTrackFormat(formatIndex)
                out += MediaTrack(
                    groupIndex = groupIndex,
                    formatIndex = formatIndex,
                    kind = kind,
                    language = format.language,
                    label = format.label,
                    isSelected = group.isTrackSelected(formatIndex),
                    channels = format.channelCount.coerceAtLeast(0),
                    isForced = format.selectionFlags and C.SELECTION_FLAG_FORCED != 0,
                    isDefault = format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0,
                )
            }
        }
        return out
    }

    fun selectTrack(track: MediaTrack) {
        val group = exo.currentTracks.groups.getOrNull(track.groupIndex) ?: return
        exo.trackSelectionParameters = exo.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, listOf(track.formatIndex))
            )
            .setTrackTypeDisabled(typeOf(track.kind), false)
            .build()
    }

    /**
     * Выключает дорожки типа целиком — нужно для «Субтитры: выкл».
     * Через `setTrackTypeDisabled`, а не пустым override: снятый override
     * вернул бы автоматический выбор, то есть субтитры включились бы снова.
     */
    fun disableTracks(kind: TrackKind) {
        exo.trackSelectionParameters = exo.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(typeOf(kind), true)
            .build()
    }

    /** Уведомление о смене набора дорожек: их список готов не сразу после load(). */
    fun onTracksChanged(listener: (List<MediaTrack>) -> Unit) {
        exo.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                listener(availableTracks())
            }
        })
    }

    private fun typeOf(kind: TrackKind): Int = when (kind) {
        TrackKind.Audio -> C.TRACK_TYPE_AUDIO
        TrackKind.Subtitle -> C.TRACK_TYPE_TEXT
    }
}
