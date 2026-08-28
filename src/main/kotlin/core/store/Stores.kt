package core.store

import core.model.SourceConfig
import core.validate.SourceHealth
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Файловое хранилище. Одна запись — один файл, имя = хост.
 * Никаких индексов и БД: источников десятки, не тысячи.
 */
abstract class JsonStore<T>(private val dir: File) {

    protected val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true   // старый файл новой версией кода
    }

    init { dir.mkdirs() }

    protected abstract fun encode(value: T): String
    protected abstract fun decode(text: String): T
    protected abstract fun keyOf(value: T): String

    private fun fileFor(key: String) = File(dir, safe(key) + ".json")

    fun load(key: String): T? {
        val f = fileFor(key)
        if (!f.isFile) return null
        return runCatching { decode(f.readText()) }.getOrElse {
            // Битый файл не должен ронять приложение: отводим в сторону.
            runCatching { f.renameTo(File(f.parentFile, f.name + ".corrupt")) }
            null
        }
    }

    /** Запись через временный файл: обрыв не оставит half-written JSON. */
    fun save(value: T) {
        val target = fileFor(keyOf(value))
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(encode(value))
        runCatching {
            Files.move(tmp.toPath(), target.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.recoverCatching {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }.getOrThrow()
    }

    fun delete(key: String): Boolean = fileFor(key).delete()

    fun keys(): List<String> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.map { it.name.removeSuffix(".json") }.orEmpty().sorted()

    private fun safe(key: String) = key.lowercase().replace(Regex("""[^a-z0-9.\-_]"""), "_")
}

class HealthStore(dir: File) : JsonStore<SourceHealth>(dir) {
    override fun encode(value: SourceHealth) = json.encodeToString(SourceHealth.serializer(), value)
    override fun decode(text: String) = json.decodeFromString(SourceHealth.serializer(), text)
    override fun keyOf(value: SourceHealth) = value.host

    /** Здоровье нового источника, а не null — вызывающему не нужен особый случай. */
    fun loadOrFresh(host: String): SourceHealth = load(host) ?: SourceHealth(host = host)
}

class ConfigStore(dir: File) : JsonStore<SourceConfig>(dir) {
    override fun encode(value: SourceConfig) = json.encodeToString(SourceConfig.serializer(), value)
    override fun decode(text: String) = json.decodeFromString(SourceConfig.serializer(), text)
    override fun keyOf(value: SourceConfig) = value.host
}
