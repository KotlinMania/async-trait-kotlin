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
                maven { url = uri("../../library/build/verification-repository") }
            }
            filter { includeModuleByRegex("io.github.kotlinmania", "async-trait-kotlin.*") }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../../gradle/libs.versions.toml")) }
    }
}
rootProject.name = "async-trait-consumer"
