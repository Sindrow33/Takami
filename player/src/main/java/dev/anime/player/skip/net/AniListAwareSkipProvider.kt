package dev.anime.player.skip.net

import dev.anime.player.skip.SkipProvider
import dev.anime.player.skip.SkipSegment

/**
 * Оборачивает [AniSkipProvider] маппингом AniList id -> MAL id, потому что каталог
 * приложения оперирует AniList id, а AniSkip индексирует эпизоды по MAL id.
 *
 * ВАЖНО: параметр [SkipProvider.segments] здесь принимает AniList id, а не MAL id
 * (несмотря на имя `malId` в общей сигнатуре) — это единственная точка входа,
 * которая должна использоваться остальным приложением; сырой [AniSkipProvider]
 * работать без маппинга не может.
 */
class AniListAwareSkipProvider(
    private val mapper: AniListMalMapper = CachingAniListMalMapper(),
    private val delegate: SkipProvider = AniSkipProvider(),
) : SkipProvider {

    override val name = "aniskip+anilist"

    override suspend fun segments(malId: Int?, episode: Int, durationMs: Long): List<SkipSegment> {
        val aniListId = malId ?: return emptyList()
        val resolvedMalId = runCatching { mapper.malIdFor(aniListId) }.getOrNull() ?: return emptyList()
        return delegate.segments(resolvedMalId, episode, durationMs)
    }
}
