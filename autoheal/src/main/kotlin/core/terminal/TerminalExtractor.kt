package core.terminal

import core.model.SourceConfig
import core.model.TerminalContent
import core.model.TerminalExpect
import core.parse.Dom

/**
 * Единая точка входа терминального слоя: по типу ожидаемого контента
 * выбирает резолвер. Раньше `StandardExtractor` на RequestKind.CONTENT
 * возвращал заглушку «терминальный слой не подключён» — теперь этот
 * класс её заменяет.
 *
 * Если конфиг молчит о типе, он выводится из профиля источника:
 * VIDEO → потоки, MANGA → картинки, NOVEL → текст.
 */
class TerminalExtractor(
    private val images: ImageResolver = ImageResolver(),
    private val text: TextResolver = TextResolver(),
    private val streams: StreamResolver = StreamResolver(),
) {

    fun extract(dom: Dom, config: SourceConfig, url: String): TerminalContent {
        val expect = config.terminal?.expect ?: defaultExpect(config)
        return when (expect) {
            TerminalExpect.IMAGES -> images.resolve(dom, config.terminal, url)
            TerminalExpect.TEXT -> text.resolve(dom, config.terminal)
            TerminalExpect.VIDEO -> streams.resolve(dom, config.terminal, url)
        }
    }

    private fun defaultExpect(config: SourceConfig): TerminalExpect =
        when (config.profile.kind) {
            core.model.MediaKind.VIDEO -> TerminalExpect.VIDEO
            core.model.MediaKind.MANGA -> TerminalExpect.IMAGES
            core.model.MediaKind.NOVEL -> TerminalExpect.TEXT
            // Тело новости — такой же текст, как глава ранобэ:
            // отдельного извлекателя под него не нужно.
            core.model.MediaKind.NEWS -> TerminalExpect.TEXT
        }
}
