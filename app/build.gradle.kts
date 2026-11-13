plugins {
    // Kotlin übersetzt das Android-Bauwerkzeug seit AGP 9 selbst; ein eigenes Kotlin-Plugin ist nicht mehr nötig.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/** Aufgaben, die eine Release-APK packen oder signieren (z. B. packageRelease); nur sie brauchen den Schlüssel. */
val releasePackagingTask = Regex("(package|sign).*Release.*")

android {
    namespace = "com.tablettenchecker.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tablettenchecker.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    // Der Signaturschlüssel kommt aus Umgebungsvariablen (auf GitHub aus den Secrets).
    // Auf GitHub (CI=true) ist er Pflicht, sobald eine Release-APK gepackt würde – so wird nie eine
    // unsignierte APK veröffentlicht.
    val keystoreFile = System.getenv("KEYSTORE_FILE")
    if (System.getenv("CI") == "true" && keystoreFile == null) {
        gradle.taskGraph.whenReady {
            if (allTasks.any { releasePackagingTask.matches(it.name) }) {
                throw GradleException("Signaturschlüssel fehlt (KEYSTORE_FILE). Ohne Signatur wird auf GitHub nicht gebaut.")
            }
        }
    }
    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storeType = "PKCS12"
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8 entfernt ungenutzten Code und ungenutzte Ressourcen und optimiert den Rest.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
