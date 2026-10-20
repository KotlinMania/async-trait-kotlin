import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
}

abstract class VerifyAndroidSelection : DefaultTask() {
    @get:Input
    abstract val selection: MapProperty<String, String>

    @get:Input
    abstract val libraryVersion: Property<String>

    @get:OutputFile
    abstract val receipt: RegularFileProperty

    @TaskAction
    fun verify() {
        val expected =
            mapOf(
                "group" to "io.github.kotlinmania",
                "module" to "async-trait-kotlin-android",
                "version" to libraryVersion.get(),
                "org.jetbrains.kotlin.platform.type" to "androidJvm",
                "org.gradle.libraryelements" to "aar",
                "org.gradle.jvm.environment" to "android",
            )
        val actual = selection.get()
        expected.forEach { (key, value) ->
            check(actual[key] == value) { "$key=${actual[key]}, expected $value; selected $actual" }
        }
        receipt.get().asFile.apply {
            parentFile.mkdirs()
            writeText(actual.toSortedMap().entries.joinToString("\n", postfix = "\n") { "${it.key}=${it.value}" })
        }
    }
}

val producerProperties =
    providers.fileContents(layout.projectDirectory.file("../../gradle.properties")).asText.map { text ->
        Properties().apply { text.reader().use { load(it) } }
    }
val testedVersion =
    providers.gradleProperty("libraryVersion").orElse(producerProperties.map { it.getProperty("project.version") })

kotlin {
    jvmToolchain(producerProperties.get().getProperty("jvm.toolchain").toInt())
    compilerOptions { allWarningsAsErrors.set(true) }
    android {
        namespace = "io.github.kotlinmania.asynctrait.androidconsumer"
        compileSdk = producerProperties.get().getProperty("android.compileSdk").toInt()
        minSdk = producerProperties.get().getProperty("android.minSdk").toInt()
    }
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.kotlinmania:async-trait-kotlin:${testedVersion.get()}")
        }
    }
}

kotlin.targets.named("android") {
    compilations.named("main") {
        val consumerCompile = compileTaskProvider
        val compileDependencies = configurations.getByName(compileDependencyConfigurationName)
        val selected =
            compileDependencies.incoming.resolutionResult.rootComponent.map { root ->
                val dependency =
                    root.dependencies.filterIsInstance<ResolvedDependencyResult>().single {
                        val requested = it.requested
                        requested is ModuleComponentSelector && requested.group == "io.github.kotlinmania" &&
                            requested.module == "async-trait-kotlin"
                    }
                val variant = dependency.resolvedVariant.externalVariant.orElse(dependency.resolvedVariant)
                val owner = variant.owner as ModuleComponentIdentifier
                variant.attributes.keySet().associate { it.name to variant.attributes.getAttribute(it).toString() } +
                    mapOf("group" to owner.group, "module" to owner.module, "version" to owner.version)
            }
        tasks.register<VerifyAndroidSelection>("verifyAndroidPublication") {
            selection.set(selected)
            libraryVersion.set(testedVersion)
            receipt.set(layout.buildDirectory.file("reports/publication/android.properties"))
            dependsOn(consumerCompile)
        }
    }
}
