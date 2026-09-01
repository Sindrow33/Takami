package core.engine

import core.extract.RequestKind
import core.extract.StandardExtractor
import core.heal.HealResult
import core.heal.Healer
import core.heal.SignatureCapture
import core.model.ParsedPayload
import core.model.SourceConfig
import core.parse.Dom
import core.parse.JsoupParser
import core.parse.Parser
import core.store.Outcome
import core.store.SourceRegistry
import core.validate.ContractReport
import core.validate.StandardContractChecker
import core.validate.Verdict

/**
 * Один разбор от начала до конца: конфиг из реестра → извлечение →
 * проверка контракта → при поломке ремонт и голосование → запись
 * состояния. Порядок здесь неслучаен и не должен дублироваться выше:
 * именно из-за размазанного порядка предыдущая версия то чинила
 * по устаревшему конфигу, то теряла отпечатки.
 */
class ParseEngine(
    private val registry: SourceRegistry,
    private val parser: Parser = JsoupParser(),
    private val extractor: StandardExtractor = StandardExtractor(),
    private val healer: Healer = Healer(),
) {

    data class Result(
        val payload: ParsedPayload,
        val report: ContractReport,
        val outcome: Outcome,
        val heal: HealResult? = null,
        /** Конфиг с обновлёнными отпечатками — вызывающий обязан его сохранить. */
        val configToPersist: SourceConfig? = null,
    ) {
        val isUsable: Boolean get() = report.verdict.isUsable
    }

    fun parse(
        host: String,
        html: String,
        url: String,
        bundled: SourceConfig,
        kind: RequestKind = RequestKind.LISTING,
        truncated: Boolean = false,
        browserHeaders: Boolean = false,
    ): Result {
        val config = registry.config(host, bundled)
        val health = registry.health(host)
        val dom = parser.parse(html, url)

        val extracted = extractor.extract(kind, dom, config, url)
        val report = StandardContractChecker(config.profile)
            .check(extracted.payload, extracted.trace, health, truncated)

        // Успех — момент снять отпечатки. Ждать поломки нельзя: узла уже не будет.
        if (report.verdict.isUsable) {
            val refreshed = SignatureCapture.refresh(dom, config, report)
            val outcome = registry.record(host, report, html.length, browserHeaders)
            return Result(
                payload = extracted.payload,
                report = report,
                outcome = outcome,
                configToPersist = refreshed.takeIf { it != config },
            )
        }

        // Поломка: сначала гипотезы ремонта, потом запись состояния —
        // иначе голос уйдёт с уже испорченной статистикой здоровья.
        val heal = healer.heal(dom, config, report, health)
        if (!heal.isEmpty) registry.proposeRepair(host, config, heal.proposals, report)
        val outcome = registry.record(host, report, html.length, browserHeaders)

        return Result(extracted.payload, report, outcome, heal)
    }

    /**
     * Повторный разбор тем же движком после того, как ревизия применилась.
     * Отдельный метод, чтобы вызывающий не соблазнился рекурсией:
     * повтор допустим ровно один раз за запрос.
     */
    fun reparseAfterHeal(
        host: String, html: String, url: String, bundled: SourceConfig,
        kind: RequestKind = RequestKind.LISTING,
    ): Result? {
        val fresh = registry.config(host, bundled)
        val dom = parser.parse(html, url)
        val extracted = extractor.extract(kind, dom, fresh, url)
        val report = StandardContractChecker(fresh.profile)
            .check(extracted.payload, extracted.trace, registry.health(host))
        return if (report.verdict is Verdict.Broken) null
        else Result(extracted.payload, report, Outcome.Fine(report))
    }
}
