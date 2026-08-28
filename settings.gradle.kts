/*
 * Manga Reader module — standalone Gradle composite.
 *
 * This settings file describes ONLY the manga-reader component of a larger
 * (future) application that will also contain an auto-parser, an anime
 * player and a light-novel reader. Those other components are NOT part of
 * this checkout; the modules below are written so they can be dropped into
 * a multi-module root project unchanged (same module paths, same group id).
 *
 * NOTE: per task constraints, no build/sync has been executed against this
 * file. It is provided so the module graph is explicit and so a future
 * root project can `include` these paths verbatim.
 */

rootProject.name = "manga-reader"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

// ---- :core:* — shared with the rest of the future app -----------------
include(":core:model")
include(":core:network")
include(":core:database")

// ---- :reader:* — reading engine and UI ---------------------------------
include(":reader:engine")
include(":reader:ui")

// ---- :translate:* — the "reads in your language" layer ----------------
include(":translate:api")
include(":translate:onnx")
include(":translate:ocr")
include(":translate:mt")
include(":translate:render")
include(":translate:core")

// ---- :feature:reader — DI wiring + public module entry point ----------
include(":feature:reader")
