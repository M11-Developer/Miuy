// Top-level build file for Miyu Companion (Android)
// Kotlin 2.0.x uses the official Compose compiler Gradle plugin (org.jetbrains.kotlin.plugin.compose).
// Using "1.9.22" for that plugin id was invalid and is the reason the earlier CI job could never
// resolve plugins, so the plugin versions below are kept in lock-step with the Kotlin version.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
