buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Seit AGP 9 übersetzt das Android-Bauwerkzeug Kotlin selbst (eingebautes Kotlin) und bringt dafür
        // eine eigene, ältere Kotlin-Version mit. Diese Zeile hebt sie auf die Version aus libs.versions.toml,
        // passend zu den Kotlin-Plugins für Compose und Serialisierung.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
