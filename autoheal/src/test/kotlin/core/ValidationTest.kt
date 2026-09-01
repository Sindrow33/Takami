package core

import core.extract.RequestKind
import core.test.CatalogPage
import core.test.Fixtures
import core.test.Harness
import core.validate.IssueCode
import core.validate.Verdict
import org.junit.Assert.*
import org.junit.Test

/**
 * Сценарии из app.MainKt, переписанные как тесты: раньше они печатали
 * отчёт в консоль и требовали, чтобы кто-то его прочитал глазами.
 */
class ValidationTest {

    private val harness = Harness()

    @Test fun `каталог без изменений разбирается чисто`() {
        val r = harness.run(CatalogPage().html(), Fixtures.catalogConfig(), Fixtures.healthyHistory())
        assertTrue("ожидался Ok, получен ${r.report.verdict}", r.report.verdict is Verdict.Ok)
        assertEquals(24, r.itemCount)
        assertTrue(r.report.verdict.confidenceOrZero >= 0.90)
    }

    @Test fun `редизайн с хеш-классами ловится как поломка`() {
        val html = CatalogPage(itemClass = "css-1q7hf2n", titleClass = "css-9klm3a").html()
        val r = harness.run(html, Fixtures.catalogConfig(), Fixtures.healthyHistory())
        assertFalse("поломка не должна считаться пригодной", r.report.verdict.isUsable)
    }

    @Test fun `подмешанные рекомендации видны по падению заполненности`() {
        val html = CatalogPage(withRecommendations = true).html()
        val r = harness.run(html, Fixtures.catalogConfig(), Fixtures.healthyHistory())
        assertEquals(32, r.itemCount)
        assertTrue(
            "ожидался сигнал о падении заполненности, получено ${r.report.issues.map { it.code }}",
            r.report.issues.any {
                it.code == IssueCode.FIELD_FILL_DROP || it.code == IssueCode.LIST_TOO_LARGE
            },
        )
    }

    @Test fun `пустая выдача у нового источника не считается поломкой`() {
        val html = CatalogPage(count = 0).html()
        val r = harness.run(html, Fixtures.catalogConfig(), Fixtures.freshHistory())
        assertTrue(
            "у источника без истории пустота — это EmptyByDesign, а не Broken",
            r.report.verdict is Verdict.EmptyByDesign,
        )
    }

    @Test fun `пустая выдача при истории — поломка`() {
        val html = CatalogPage(count = 0).html()
        val r = harness.run(html, Fixtures.catalogConfig(), Fixtures.healthyHistory())
        assertTrue(r.report.verdict is Verdict.Broken)
        assertTrue(r.report.issues.any { it.code == IssueCode.LIST_EMPTY })
    }

    @Test fun `заглушки вместо обложек не рушат разбор, но отмечаются`() {
        val html = CatalogPage(brokenCovers = true).html()
        val r = harness.run(html, Fixtures.catalogConfig(), Fixtures.healthyHistory())
        assertEquals(24, r.itemCount)
        assertTrue(
            "обложка-заглушка должна давать сигнал",
            r.report.issues.any {
                it.field == "cover" &&
                    (it.code == IssueCode.FIELD_PLACEHOLDER || it.code == IssueCode.FIELD_MISSING ||
                        it.code == IssueCode.FIELD_LOW_FILL || it.code == IssueCode.FIELD_FILL_DROP)
            },
        )
    }

    @Test fun `повторяющиеся ссылки дают сигнал о дубликатах ключей`() {
        val html = CatalogPage(duplicateUrls = true).html()
        val r = harness.run(html, Fixtures.catalogConfig(), Fixtures.healthyHistory())
        assertTrue(r.report.issues.any { it.code == IssueCode.KEY_DUPLICATE })
    }

    @Test fun `фолбэк отмечается даже когда данные внешне в порядке`() {
        // Основной селектор карточки мёртв, но микроданные позволяют найти их запасным.
        val html = CatalogPage(itemClass = "renamed-card", withMicrodata = true).html()
        val r = harness.run(html, Fixtures.catalogConfig(), Fixtures.healthyHistory())
        if (r.itemCount > 0) {
            assertTrue(
                "использованный фолбэк обязан попасть в отчёт",
                r.report.issues.any { it.code == IssueCode.FALLBACK_USED } ||
                    r.report.trace?.usedFallback == true,
            )
        }
    }

    @Test fun `навигация и футер не попадают в каталог`() {
        val r = harness.run(CatalogPage().html(), Fixtures.catalogConfig(), Fixtures.healthyHistory())
        val titles = (r.payload as core.model.ParsedPayload.Listing).items.map { it.title }
        assertFalse("в выдачу попала навигация: $titles", titles.any { it == "Главная" || it == "Каталог" })
    }

    @Test fun `entry читает og-метаданные когда селекторов нет`() {
        val r = harness.run(
            CatalogPage().html(),
            Fixtures.catalogConfig().copy(listing = null, entry = core.model.EntrySpec()),
            Fixtures.freshHistory(),
            kind = RequestKind.ENTRY,
        )
        val e = (r.payload as core.model.ParsedPayload.Entry).entry
        assertEquals("Каталог аниме", e.title)
    }
}
