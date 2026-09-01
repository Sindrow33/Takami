package com.mangareader.translate.core.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StatFs
import com.mangareader.translate.api.ModelDownloadPolicy
import com.mangareader.translate.api.TranslationAvailability
import com.mangareader.translate.api.TranslationModel
import com.mangareader.translate.api.TranslationModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Модели, скачиваемые по требованию.
 *
 * Веса не лежат в APK: нативные библиотеки перевода весят по 17 МБ на
 * архитектуру, и модели поверх них сделали бы сборку, до установки
 * которой дело не доходит. Поэтому файл приезжает отдельно, один раз, и
 * дальше живёт на диске.
 *
 * Все решения — «хватит ли места», «можно ли по мобильной сети»,
 * «сколько уже скачано» — вынесены в [ModelDownloadPolicy] и проверены
 * тестами; здесь остаются только файлы, сеть и системные запросы.
 *
 * @param baseUrl откуда качать. Пока не задан — провайдер честно
 *   сообщает, что моделей нет, вместо того чтобы стучаться в
 *   несуществующий адрес и показывать «ошибка сети».
 */
class DownloadedModelProvider(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val baseUrl: String?,
    private val scope: CoroutineScope,
) : TranslationModelProvider {

    private val modelsDir: File by lazy {
        File(context.filesDir, "translate-models").apply { mkdirs() }
    }

    private val _availability = MutableStateFlow<TranslationAvailability>(
        TranslationAvailability.DeviceUnsupported("Проверка…"),
    )
    override val availability: StateFlow<TranslationAvailability> = _availability.asStateFlow()

    private var downloadJob: Job? = null

    init {
        refresh()
    }

    override fun current(): TranslationAvailability = _availability.value

    /** Пересчитывает состояние: место, сеть и наличие файлов меняются сами. */
    fun refresh() {
        if (_availability.value is TranslationAvailability.Downloading) return
        if (baseUrl.isNullOrBlank() && !allModelsPresent()) {
            _availability.value = TranslationAvailability.DeviceUnsupported(
                "Модели перевода пока не поставляются",
            )
            return
        }
        _availability.value = ModelDownloadPolicy.evaluate(
            modelsPresent = allModelsPresent(),
            freeBytes = freeSpaceBytes(),
            requiredBytes = TranslationModel.totalBytes,
            online = isOnline(),
            metered = isMetered(),
            architectureSupported = isArchitectureSupported(),
        )
    }

    override suspend fun download(allowMetered: Boolean) {
        if (downloadJob?.isActive == true) return
        val base = baseUrl?.takeIf { it.isNotBlank() } ?: return
        if (!ModelDownloadPolicy.mayStart(isOnline(), isMetered(), allowMetered)) {
            _availability.value = TranslationAvailability.DownloadFailed(
                if (!isOnline()) "Нет подключения к сети" else "Загрузка по мобильной сети не разрешена",
            )
            return
        }

        downloadJob = scope.launch(Dispatchers.IO) {
            _availability.value = TranslationAvailability.Downloading(0f)
            val total = TranslationModel.totalBytes
            var done = 0L
            for (model in TranslationModel.entries) {
                val target = File(modelsDir, model.fileName)
                if (target.isFile && target.length() > 0) {
                    done += target.length()
                    continue
                }
                val result = runCatching {
                    fetch(base.trimEnd('/') + "/" + model.fileName, target) { chunk ->
                        done += chunk
                        _availability.value = TranslationAvailability.Downloading(
                            ModelDownloadPolicy.progressOf(done, total),
                        )
                    }
                }
                if (result.isFailure) {
                    // Незавершённый файл удаляется: оставленный, он при
                    // следующем запуске выглядел бы как скачанная модель
                    // и ронял бы ONNX уже внутри чтения.
                    target.delete()
                    _availability.value = TranslationAvailability.DownloadFailed(
                        result.exceptionOrNull()?.message ?: "Не удалось скачать модель",
                    )
                    return@launch
                }
            }
            refreshAfterDownload()
        }
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        // Частично скачанное не должно пережить отмену — см. выше.
        TranslationModel.entries.forEach { File(modelsDir, it.fileName + PART_SUFFIX).delete() }
        refresh()
    }

    override fun modelPath(model: TranslationModel): String? =
        File(modelsDir, model.fileName).takeIf { it.isFile && it.length() > 0 }?.absolutePath

    override suspend fun deleteDownloaded() = withContext(Dispatchers.IO) {
        TranslationModel.entries.forEach { File(modelsDir, it.fileName).delete() }
        refresh()
    }

    private fun refreshAfterDownload() {
        _availability.value = if (allModelsPresent()) {
            TranslationAvailability.Ready
        } else {
            TranslationAvailability.DownloadFailed("Файлы модели неполные")
        }
    }

    /**
     * Скачивание в отдельный файл с переименованием в конце.
     *
     * Прямая запись в целевой файл означала бы, что оборванная загрузка
     * оставляет на диске нечто с правильным именем и неправильным
     * содержимым — и следующий запуск считал бы модель установленной.
     *
     * Заголовки сжатия здесь не выставляются сознательно: ими управляет
     * клиент, а ручной `Accept-Encoding: gzip` отключает автоматическую
     * распаковку, и тело приезжает архивом.
     */
    private fun fetch(url: String, target: File, onChunk: (Long) -> Unit) {
        val part = File(target.parentFile, target.name + PART_SUFFIX)
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Сервер ответил ${response.code}")
            val body = response.body ?: error("Пустой ответ")
            part.outputStream().buffered().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        onChunk(read.toLong())
                    }
                }
            }
        }
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
    }

    private fun allModelsPresent(): Boolean =
        TranslationModel.entries.all { model ->
            File(modelsDir, model.fileName).let { it.isFile && it.length() > 0 }
        }

    private fun freeSpaceBytes(): Long =
        runCatching { StatFs(context.filesDir.absolutePath).availableBytes }.getOrDefault(0L)

    private fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isMetered(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = manager.getNetworkCapabilities(manager.activeNetwork ?: return true) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Нативные библиотеки перевода собраны только под два ABI — те же,
     * что оставлены в сборке. На остальных модель скачается и не
     * запустится, поэтому предлагать загрузку нельзя.
     */
    private fun isArchitectureSupported(): Boolean =
        Build.SUPPORTED_ABIS.any { it in SUPPORTED_ABIS }

    private companion object {
        const val PART_SUFFIX = ".part"
        const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
        val SUPPORTED_ABIS = setOf("arm64-v8a", "x86_64")
    }
}
