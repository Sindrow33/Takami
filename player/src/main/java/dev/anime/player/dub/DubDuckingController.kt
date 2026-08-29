package dev.anime.player.dub

import dev.anime.player.core.PlayerEngine
import kotlin.math.max
import kotlin.math.min

/**
 * Приглушение оригинальной аудиодорожки, пока играет реплика AI-озвучки — задача из README
 * ("TTS-озвучка закадром с приглушением оригинала"). Логика отделена от воспроизведения самого
 * дублированного клипа (это делает Android `AudioTrack`/MediaPlayer выше, вне этого модуля):
 * контроллер только решает, какой должна быть громкость [PlayerEngine] прямо сейчас, с плавным
 * fade вместо резкого щелчка.
 *
 * Вызывается на каждый тик воспроизведения ([onTick]) с текущей позицией и тем, идёт ли сейчас
 * озвучка ([DubCoordinator.clipAt] != null).
 */
class DubDuckingController(
    private val engine: PlayerEngine,
    private val duckedVolume: Float = 0.15f,
    private val fullVolume: Float = 1f,
    private val fadeMs: Long = 250L,
) {
    private var fadeStartMs: Long = 0L
    private var fadeFromVolume: Float = fullVolume
    private var fadeToVolume: Float = fullVolume
    private var targetVolume: Float = fullVolume

    /** [nowMs] — реальное время (System.currentTimeMillis()), не позиция воспроизведения. */
    fun onTick(isDubActive: Boolean, nowMs: Long) {
        val newTarget = if (isDubActive) duckedVolume else fullVolume
        if (newTarget != targetVolume) {
            fadeFromVolume = currentVolume(nowMs)
            fadeToVolume = newTarget
            fadeStartMs = nowMs
            targetVolume = newTarget
        }
        engine.setVolume(currentVolume(nowMs))
    }

    private fun currentVolume(nowMs: Long): Float {
        if (fadeMs <= 0L) return targetVolume
        val elapsed = (nowMs - fadeStartMs).coerceIn(0L, fadeMs)
        val t = elapsed.toFloat() / fadeMs.toFloat()
        val v = fadeFromVolume + (fadeToVolume - fadeFromVolume) * t
        return min(max(v, 0f), 1f)
    }
}
