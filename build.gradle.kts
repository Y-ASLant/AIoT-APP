import org.gradle.kotlin.dsl.register

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
