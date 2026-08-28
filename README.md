# autoheal

Самовосстанавливающийся парсер каталогов: HTML -> Jsoup -> извлечение по конфигу ->
валидация контракта -> вердикт -> ремонт селекторов с голосованием, испытательным
сроком и откатом.

## Слои
- `core/model`    — модель данных и конфигов источника
- `core/parse`    — обёртка над Jsoup, UrlTools
- `core/extract`  — StandardExtractor, лестница селекторов, ExtractionTrace
- `core/validate` — контракт, Issue/Verdict, SourceHealth, ErrorBudget
- `core/store`    — JSON-хранилища: health, config, revisions, budget
- `core/heal`     — сигнатуры элементов (в работе)
- `core/net`      — HTTP (пока исключён из сборки)
- `app`           — прогоны сценариев и демо

## Сборка (без Gradle, kotlinc напрямую)
    ./libs/fetch.sh
    ./build.sh

## Точки входа
- `app.MainKt`         — шесть регрессионных сценариев
- `app.MemoryKt`       — накопление истории по дням
- `app.BudgetProbeKt`  — калибровка порогов бюджета ошибок
- `app.RevisionDemoKt` — голосование, испытательный срок, откат
- `app.RegistryDemoKt` — состояние переживает перезапуск
