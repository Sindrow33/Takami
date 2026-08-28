package moe.scenesearch.resolver

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.scenesearch.api.SceneIdentity

data class IndexEntry(
    val anilistId: Int? = null,
    val malId: Int? = null,
    val shikimoriId: Int? = null,
    val titles: List<String> = emptyList(),
)

/**
 * AniList отдаёт romaji, english и native, а русские источники хранят русские названия.
 * Индекс превращает один идентификатор в десяток синонимов со всех трекеров.
 */
interface OfflineTitleIndex {
    fun expand(identity: SceneIdentity): List<String>

    fun lookup(anilistId: Int?, malId: Int?): IndexEntry?

    companion object {
        val Empty: OfflineTitleIndex = object : OfflineTitleIndex {
            override fun expand(identity: SceneIdentity): List<String> = emptyList()
            override fun lookup(anilistId: Int?, malId: Int?): IndexEntry? = null
        }
    }
}

class InMemoryTitleIndex(entries: List<IndexEntry>) : OfflineTitleIndex {

    private val byAnilist = HashMap<Int, IndexEntry>()
    private val byMal = HashMap<Int, IndexEntry>()

    init {
        for (e in entries) {
            val a = e.anilistId
            if (a != null) byAnilist[a] = e
            val m = e.malId
            if (m != null) byMal[m] = e
        }
    }

    override fun lookup(anilistId: Int?, malId: Int?): IndexEntry? {
        if (anilistId != null) {
            val hit = byAnilist[anilistId]
            if (hit != null) return hit
        }
        if (malId != null) return byMal[malId]
        return null
    }

    override fun expand(identity: SceneIdentity): List<String> =
        lookup(identity.anilistId, identity.malId)?.titles.orEmpty()

    val size: Int get() = byAnilist.size
}

/**
 * Парсер базы manami-project/anime-offline-database.
 * Один JSON кладётся в assets и обновляется раз в неделю.
 */
object AnimeOfflineDatabaseParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val anilistRe = Regex("""anilist\.co/anime/(\d+)""")
    private val malRe = Regex("""myanimelist\.net/anime/(\d+)""")
    private val shikiRe = Regex("""shikimori\.\w+/animes/[a-z]?(\d+)""")

    fun parse(raw: String): List<IndexEntry> {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyList()
        val data = root["data"] as? JsonArray ?: return emptyList()
        val out = ArrayList<IndexEntry>(data.size)

        for (element in data) {
            val obj = element as? JsonObject ?: continue

            val sourcesArray = obj["sources"] as? JsonArray
            val sources = StringBuilder()
            if (sourcesArray != null) {
                for (s in sourcesArray) {
                    val v = s.jsonPrimitive.contentOrNull
                    if (v != null) {
                        sources.append(v)
                        sources.append(' ')
                    }
                }
            }
            val src = sources.toString()

            val titles = ArrayList<String>()
            val main = obj["title"]?.jsonPrimitive?.contentOrNull
            if (main != null) titles.add(main)
            val syn = obj["synonyms"] as? JsonArray
            if (syn != null) {
                for (s in syn) {
                    val v = s.jsonPrimitive.contentOrNull
                    if (v != null) titles.add(v)
                }
            }

            val anilistId = anilistRe.find(src)?.groupValues?.get(1)?.toIntOrNull()
            val malId = malRe.find(src)?.groupValues?.get(1)?.toIntOrNull()
            if (anilistId == null && malId == null) continue

            out.add(
                IndexEntry(
                    anilistId = anilistId,
                    malId = malId,
                    shikimoriId = shikiRe.find(src)?.groupValues?.get(1)?.toIntOrNull(),
                    titles = titles.filter { it.isNotBlank() }.distinct(),
                )
            )
        }
        return out
    }

    fun index(raw: String): OfflineTitleIndex = InMemoryTitleIndex(parse(raw))
}
