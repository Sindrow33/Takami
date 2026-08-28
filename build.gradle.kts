// Root build script for the manga-reader module set.
// No build has been executed as part of this task — sources only.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    group = "com.mangareader"
    version = "0.1.0-module"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
