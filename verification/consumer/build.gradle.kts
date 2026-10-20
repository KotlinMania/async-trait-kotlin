import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

abstract class VerifyPublishedVariant : DefaultTask() {
    @get:Input
    abstract val selection: MapProperty<String, String>

    @get:Input
    abstract val expected: MapProperty<String, String>

    @get:OutputFile
    abstract val receipt: RegularFileProperty

    @TaskAction
    fun verify() {
        val actual = selection.get()
        expected.get().forEach { (key, value) ->
            check(actual[key] == value) { "$path selected $key=${actual[key]}, expected $value; $actual" }
        }
        receipt.get().asFile.apply {
            parentFile.mkdirs()
            writeText(actual.toSortedMap().entries.joinToString("\n", postfix = "\n") { "${it.key}=${it.value}" })
        }
    }
}

val libraryVersion = providers.gradleProperty("libraryVersion").orElse(
    providers.fileContents(layout.projectDirectory.file("../../gradle.properties")).asText.map { text ->
        text.lineSequence().first { it.startsWith("project.version=") }.substringAfter('=')
    },
)

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
    compilerOptions { allWarningsAsErrors = true }
    jvm()
    android {
        namespace = "io.github.kotlinmania.asynctrait.consumer"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder { sourceSetTreeName = "test" }
    }
    js { nodejs() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { nodejs() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi { nodejs() }
    linuxX64()
    linuxArm64()
    mingwX64()
    androidNativeArm64()
    androidNativeX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.kotlinmania:async-trait-kotlin:${libraryVersion.get()}")
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}

extensions.configure<NodeJsEnvSpec> { version = libs.versions.node.get() }
extensions.configure<WasmNodeJsEnvSpec> { version = libs.versions.node.get() }
extensions.configure<YarnRootEnvSpec> { version = libs.versions.yarn.get() }
extensions.configure<WasmYarnRootEnvSpec> { version = libs.versions.yarn.get() }
val npmResolutions = mapOf(
    "diff" to libs.versions.npmDiff.get(),
    "fast-uri" to libs.versions.npmFastUri.get(),
    "serialize-javascript" to libs.versions.npmSerializeJavascript.get(),
    "webpack" to libs.versions.webpack.get(),
    "follow-redirects" to libs.versions.npmFollowRedirects.get(),
    "lodash" to libs.versions.npmLodash.get(),
    "ajv" to libs.versions.npmAjv.get(),
    "brace-expansion" to libs.versions.npmBraceExpansion.get(),
    "flatted" to libs.versions.npmFlatted.get(),
    "minimatch" to libs.versions.npmMinimatch.get(),
    "picomatch" to libs.versions.npmPicomatch.get(),
    "qs" to libs.versions.npmQs.get(),
    "socket.io-parser" to libs.versions.npmSocketIoParser.get(),
    "ws" to libs.versions.npmWs.get(),
    "@types/estree" to libs.versions.npmTypesEstree.get(),
    "@types/node" to libs.versions.npmTypesNode.get(),
    "baseline-browser-mapping" to libs.versions.npmBaselineBrowserMapping.get(),
    "caniuse-lite" to libs.versions.npmCaniuseLite.get(),
    "electron-to-chromium" to libs.versions.npmElectronToChromium.get(),
    "enhanced-resolve" to libs.versions.npmEnhancedResolve.get(),
    "mime-db" to libs.versions.npmMimeDb.get(),
    "node-releases" to libs.versions.npmNodeReleases.get(),
    "terser" to libs.versions.npmTerser.get(),
    "terser-webpack-plugin" to libs.versions.npmTerserWebpackPlugin.get(),
    "webpack-sources" to libs.versions.npmWebpackSources.get(),
    "ua-parser-js" to libs.versions.npmUaParserJs.get(),
    "tmp" to libs.versions.npmTmp.get(),
    "body-parser" to libs.versions.npmBodyParser.get(),
    "js-yaml" to libs.versions.npmJsYaml.get(),
    "browserslist" to libs.versions.npmBrowserslist.get(),
)
listOf(
    rootProject.extensions.getByType<YarnRootExtension>(),
    rootProject.extensions.getByType<WasmYarnRootExtension>(),
).forEach { yarn ->
    npmResolutions.forEach { (name, version) ->
        yarn.resolution(name, version)
        yarn.resolution("**/$name", version)
    }
}
listOf(
    rootProject.extensions.getByType<NodeJsRootExtension>(),
    rootProject.extensions.getByType<WasmNodeJsRootExtension>(),
).forEach { node ->
    node.versions.karma.version = libs.versions.karma.get()
    node.versions.mocha.version = libs.versions.mocha.get()
    node.versions.kotlinWebHelpers.version = libs.versions.kotlinWebHelpers.get()
    node.versions.webpackCli.version = libs.versions.webpackCli.get()
    node.versions.webpack.version = libs.versions.webpack.get()
    node.versions.karmaWebpack.version = "file:${rootProject.file("../../gradle/npm/karma-webpack").invariantSeparatorsPath}"
}

val verifyPublication = tasks.register("verifyPublication")
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin.targets.configureEach {
    if (platformType != KotlinPlatformType.common) {
        val targetName = name
        val expectedAttributes = mutableMapOf("org.jetbrains.kotlin.platform.type" to platformType.name)
        if (this is KotlinNativeTarget) expectedAttributes["org.jetbrains.kotlin.native.target"] = konanTarget.name
        if (this is KotlinJsIrTarget) {
            wasmTargetType?.let { expectedAttributes["org.jetbrains.kotlin.wasm.target"] = it.name.lowercase() }
        }
        if (platformType == KotlinPlatformType.androidJvm) expectedAttributes["org.gradle.libraryelements"] = "aar"
        if (platformType == KotlinPlatformType.jvm) expectedAttributes["org.gradle.libraryelements"] = "jar"
        compilations.named("main") {
            val selected = configurations.getByName(compileDependencyConfigurationName).incoming.resolutionResult.rootComponent.map { root ->
                val dependency = root.dependencies.filterIsInstance<ResolvedDependencyResult>().single {
                    val requested = it.requested
                    requested is ModuleComponentSelector && requested.group == "io.github.kotlinmania" &&
                        requested.module == "async-trait-kotlin"
                }
                val variant = dependency.resolvedVariant.externalVariant.orElse(dependency.resolvedVariant)
                val owner = variant.owner as ModuleComponentIdentifier
                variant.attributes.keySet().associate { it.name to variant.attributes.getAttribute(it).toString() } +
                    mapOf("module" to owner.module, "version" to owner.version, "group" to owner.group,
                        "variant" to variant.displayName)
            }
            val verifyVariant = tasks.register<VerifyPublishedVariant>("verify${targetName.replaceFirstChar { it.uppercase() }}Variant") {
                selection.set(selected)
                expected.set(expectedAttributes + mapOf("module" to "async-trait-kotlin-${targetName.lowercase()}",
                    "group" to "io.github.kotlinmania", "version" to libraryVersion.get()))
                receipt.set(layout.buildDirectory.file("reports/publication/$targetName.properties"))
            }
            verifyPublication.configure { dependsOn(verifyVariant) }
        }
    }
    compilations.configureEach {
        val compile = compileTaskProvider
        verifyPublication.configure { dependsOn(compile) }
        if (platformType == KotlinPlatformType.js || platformType == KotlinPlatformType.wasm) {
            val lock = if (platformType == KotlinPlatformType.js) "kotlinUpgradeYarnLock" else "kotlinWasmUpgradeYarnLock"
            compile.configure { dependsOn(tasks.matching { it.name == lock }) }
        }
    }
}
kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    binaries.configureEach {
        val link = linkTaskProvider
        verifyPublication.configure { dependsOn(link) }
    }
}
