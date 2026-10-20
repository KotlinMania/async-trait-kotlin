pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository {
                maven { url = uri("../../build/verification-android-repository") }
            }
            filter {
                includeModule("io.github.kotlinmania", "async-trait-kotlin")
                includeModule("io.github.kotlinmania", "async-trait-kotlin-android")
            }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "async-trait-android-consumer"
