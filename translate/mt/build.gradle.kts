plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mangareader.translate.mt"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Deliberately depends on :translate:api and :core:database ONLY —
    // no dependency on :reader:*. This module is shared verbatim with the
    // (future, separate) light-novel reader component; it must not know
    // that manga pages, bitmaps or panels exist. See ARCHITECTURE.md
    // "Cross-component sharing".
    api(project(":translate:api"))
    implementation(project(":core:database"))

    implementation(libs.mlkit.translate)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
