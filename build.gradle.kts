plugins {
    base
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlinx.benchmark) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

tasks.register("test") { dependsOn(":library:test") }
tasks.named("check") { dependsOn(":library:check") }
tasks.named("build") { setDependsOn(listOf(":library:build")) }
