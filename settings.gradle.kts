pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "anime-player"
include(":player", ":demo", ":app", ":autoheal")

// ---- manga-reader (ветка manga-reader) --------------------------------
include(":core:design", ":core:model", ":core:network", ":core:database")
include(":reader:engine", ":reader:ui")
include(":translate:api", ":translate:onnx", ":translate:ocr", ":translate:mt", ":translate:render", ":translate:core")
include(":feature:reader")

// ---- источник страниц поверх автопарсера --------------------------------
include(":source:web")
