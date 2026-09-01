plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mangareader.translate.onnx"
    compileSdk = 35

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

    // ONNX model assets (comic-text-detector.onnx, lama_inpaint_int8.onnx)
    // are expected to be provided at app-assembly time by the host project
    // via this module's `src/main/assets/models/`. They are NOT vendored
    // in this checkout (binary weights don't belong in source control);
    // see ARCHITECTURE.md "Model assets" for the expected file names.
    androidResources {
        noCompress += listOf("onnx")
    }
}

dependencies {
    api(project(":translate:api"))
    implementation(libs.onnxruntime.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
}
