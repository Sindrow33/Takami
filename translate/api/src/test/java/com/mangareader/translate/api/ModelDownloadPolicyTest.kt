package com.mangareader.translate.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelDownloadPolicyTest {

    private val required = TranslationModel.totalBytes
    private val plenty = required + ModelDownloadPolicy.FREE_SPACE_MARGIN_BYTES + 1

    @Test
    fun `модели на месте — перевод готов, ничего не спрашиваем`() {
        val state = ModelDownloadPolicy.evaluate(
            modelsPresent = true,
            freeBytes = 0,
            requiredBytes = required,
            online = false,
            metered = true,
            architectureSupported = true,
        )
        assertTrue(state.isReady, "скачанная модель работает и без сети, и без свободного места")
    }

    @Test
    fun `нехватка места видна до загрузки, а не на девяноста процентах`() {
        val state = ModelDownloadPolicy.evaluate(
            modelsPresent = false,
            freeBytes = required,
            requiredBytes = required,
            online = true,
            metered = false,
            architectureSupported = true,
        )
        assertTrue(
            state is TranslationAvailability.DeviceUnsupported,
            "места ровно под модель мало: нужен запас, иначе устройство остаётся с нулём",
        )
        assertFalse(state.canDownload, "предлагать скачать то, что не поместится, нечестно")
    }

    @Test
    fun `неподдерживаемый процессор не предлагает скачивание`() {
        val state = ModelDownloadPolicy.evaluate(
            modelsPresent = false,
            freeBytes = plenty,
            requiredBytes = required,
            online = true,
            metered = false,
            architectureSupported = false,
        )
        assertTrue(state is TranslationAvailability.DeviceUnsupported)
        assertFalse(state.canDownload)
    }

    @Test
    fun `без сети причина названа и попытка возможна позже`() {
        val state = ModelDownloadPolicy.evaluate(
            modelsPresent = false,
            freeBytes = plenty,
            requiredBytes = required,
            online = false,
            metered = false,
            architectureSupported = true,
        )
        assertTrue(state is TranslationAvailability.DownloadFailed)
        assertTrue(state.canDownload, "сеть появится — кнопка должна работать без перезапуска")
    }

    @Test
    fun `мобильная сеть не запрещает загрузку, но предупреждает`() {
        val state = ModelDownloadPolicy.evaluate(
            modelsPresent = false,
            freeBytes = plenty,
            requiredBytes = required,
            online = true,
            metered = true,
            architectureSupported = true,
        )
        val needs = state as TranslationAvailability.NeedsDownload
        assertTrue(needs.meteredWarning)
        assertEquals(required, needs.sizeBytes, "размер показывается до начала, а не после")
    }

    @Test
    fun `по мобильной сети без согласия не начинаем`() {
        assertFalse(ModelDownloadPolicy.mayStart(online = true, metered = true, allowMetered = false))
        assertTrue(ModelDownloadPolicy.mayStart(online = true, metered = true, allowMetered = true))
        assertFalse(
            ModelDownloadPolicy.mayStart(online = false, metered = false, allowMetered = true),
            "согласие на трафик не заменяет наличие сети",
        )
    }

    @Test
    fun `неизвестный размер даёт null, а не вечный ноль`() {
        assertNull(
            ModelDownloadPolicy.progressOf(downloadedBytes = 100, totalBytes = 0),
            "полоса, стоящая на нуле, выглядит как зависшая загрузка",
        )
        assertEquals(0.5f, ModelDownloadPolicy.progressOf(50, 100))
        assertEquals(1f, ModelDownloadPolicy.progressOf(200, 100), "переполнение не должно давать больше единицы")
    }

    @Test
    fun `размер показывается человеку, а не в байтах`() {
        assertEquals("75 МБ", ModelDownloadPolicy.formatSize(75L * 1024 * 1024))
        assertEquals("512 КБ", ModelDownloadPolicy.formatSize(512L * 1024))
    }

    @Test
    fun `состояние загрузки не предлагает вторую загрузку`() {
        assertFalse(
            TranslationAvailability.Downloading(0.3f).canDownload,
            "две параллельные загрузки одного файла — удвоенный трафик и гонка за файл",
        )
        assertFalse(TranslationAvailability.Ready.canDownload)
        assertTrue(TranslationAvailability.DownloadFailed("сеть пропала").canDownload)
    }

    @Test
    fun `общий размер моделей считается по всем нужным`() {
        assertEquals(
            TranslationModel.entries.sumOf { it.approxBytes },
            TranslationModel.totalBytes,
        )
        assertTrue(TranslationModel.totalBytes > 0)
    }
}
