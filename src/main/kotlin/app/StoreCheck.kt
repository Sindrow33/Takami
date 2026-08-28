package app

import core.store.ConfigStore
import core.store.HealthStore
import core.test.Fixtures
import java.io.File

fun main() {
    val root = File(System.getProperty("java.io.tmpdir"), "autoheal-store")
    root.deleteRecursively()
    val health = HealthStore(File(root, "health"))
    val configs = ConfigStore(File(root, "config"))
    var bad = 0

    fun check(name: String, ok: Boolean, detail: String = "") {
        println(if (ok) "  ✔ $name" else "  ✘ $name $detail".also { bad++ })
    }

    println("хранилище: $root")

    val h0 = Fixtures.healthyHistory()
    health.save(h0)
    val h1 = health.load(h0.host)
    check("health: круговорот", h1 == h0, "\n     было: $h0\n     стало: $h1")
    check("health: fieldFill сохранился", h1?.fieldFill == h0.fieldFill)
    check("health: новый источник без файла", health.loadOrFresh("нет.такого").successCount == 0)

    val c0 = Fixtures.catalogConfig()
    configs.save(c0)
    val c1 = configs.load(c0.host)
    check("config: круговорот", c1 == c0)
    check("config: профиль", c1?.profile == c0.profile)
    check("config: диапазоны", c1?.profile?.listingSizeRange == c0.profile.listingSizeRange)
    check("config: лестница селекторов",
        c1?.listing?.fields?.get("title")?.ladder == c0.listing?.fields?.get("title")?.ladder)

    val corrupt = File(root, "health/${h0.host}.json")
    corrupt.writeText("{ это не json")
    check("битый файл не роняет", health.load(h0.host) == null)
    check("битый файл отведён", File(root, "health/${h0.host}.json.corrupt").isFile)

    configs.save(c0.copy(notes = listOf("вторая запись")))
    check("перезапись", configs.load(c0.host)?.notes == listOf("вторая запись"))
    check("список ключей", configs.keys() == listOf(c0.host))

    println(if (bad == 0) "хранилище работает" else "провалено проверок: $bad")
    println("\n--- health.json ---")
    println(File(root, "config/${c0.host}.json").readText().lines().take(20).joinToString("\n"))
}
