import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.tasks.testing.Test
import org.w3c.dom.Element
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

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

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testResults: DirectoryProperty

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
        val expectedTests =
            setOf(
                "expandsRealAsyncMethodsThroughThePublishedResultApi",
                "expandsThroughThePublishedThrowingApiAndQuoteContract",
                "preservesMalformedArgumentDiagnosticsThroughPublishedDependencies",
            )
        val executed = mutableSetOf<String>()
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        testResults.get().asFile.listFiles { file -> file.extension == "xml" }.orEmpty().forEach { file ->
            val cases = parser.parse(file).getElementsByTagName("testcase")
            for (index in 0 until cases.length) {
                val case = cases.item(index) as Element
                val name = case.getAttribute("name")
                if (case.getAttribute("classname").endsWith("consumer.PublishedApiTest") && name in expectedTests) {
                    check(listOf("failure", "error", "skipped").none { case.getElementsByTagName(it).length > 0 }) {
                        "Required consumer test did not pass: $name"
                    }
                    check(executed.add(name)) { "Duplicate consumer test result: $name" }
                }
            }
        }
        check(executed == expectedTests) { "Required consumer tests did not execute: ${expectedTests - executed}" }
        receipt.get().asFile.apply {
            parentFile.mkdirs()
            val result = actual + ("executed_tests" to executed.sorted().joinToString(","))
            writeText(result.toSortedMap().entries.joinToString("\n", postfix = "\n") { "${it.key}=${it.value}" })
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
    jvmToolchain(libs.versions.jvm.get().toInt())
    compilerOptions { allWarningsAsErrors.set(true) }
    android {
        namespace = "io.github.kotlinmania.asynctrait.androidconsumer"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder { sourceSetTreeName = "test" }
    }
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.kotlinmania:async-trait-kotlin:${testedVersion.get()}")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

val consumerTests =
    tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }
consumerTests.configureEach {
    outputs.upToDateWhen { false }
    testLogging { events("passed", "failed", "skipped") }
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
            testResults.set(
                providers.provider {
                    check(consumerTests.size == 1) { "Expected one Android host test runner" }
                    consumerTests.single().reports.junitXml.outputLocation.get()
                },
            )
            dependsOn(consumerCompile, consumerTests)
        }
    }
}
