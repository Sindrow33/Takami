plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.takami.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.takami.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.4.1"
    }
    buildTypes {
        debug { isMinifyEnabled = false }
        release { isMinifyEnabled = false }
    }

    /*
     * Нативные библиотеки перевода (ONNX Runtime + translate JNI) весят
     * по ~17 МБ на архитектуру. Со всеми четырьмя ABI в одном APK сборка
     * распухла с 11 до 186 МБ — столько никто ставить не будет.
     * Оставляем только реальные ABI устройств; x86 нужен эмулятору,
     * поэтому он тоже здесь, но 32-битный ARM уже нигде не встречается.
     */
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/*.kotlin_module",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":autoheal"))
    implementation(project(":core:design"))
    implementation(project(":core:model"))
    implementation(project(":reader:engine"))
    implementation(project(":feature:reader"))
    implementation(project(":source:web"))

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    // Выбор папки с локальным контентом через SAF: filesDir пользователю
    // недоступен, положить туда файлы с телефона нельзя.
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
