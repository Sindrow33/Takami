plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mangareader.feature.reader"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
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
    // The ONLY module the host application is expected to depend on,
    // together with :core:model for the public contract types. Everything
    // below this line is an implementation detail of the reader component.
    api(project(":core:model"))

    implementation(project(":core:network"))
    // :core:database — только Room-конвертеры для кеша переводов;
    // читалка их не использует, а транзитивный Room тянет в APK
    // рантайм и сгенерированный код. Вернётся вместе со слоем перевода.
    implementation(project(":core:design"))
    implementation(project(":reader:engine"))
    implementation(project(":reader:ui"))
    // Из всего слоя перевода читалке нужен ровно :translate:api —
    // enum TranslationMode и типы блоков. Реализация (ONNX-рантайм,
    // ML Kit, Room-кеш переводов) НЕ подключается: она не
    // инстанцируется, но тянула в APK ~33 МБ нативных библиотек и
    // десятки МБ dex. Подключать обратно — вместе с реальным
    // включением перевода и только через доставку по требованию,
    // см. docs/manga-reader/TRANSLATION-LAYER.md.
    implementation(project(":translate:api"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
