import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.plugins.signing.Sign
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.ExecutionTaskHolder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.konan.target.Family
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64
import java.util.UUID

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    `maven-publish`
    signing
}

group = providers.gradleProperty("project.group").getOrElse("io.github.kotlinmania")
version = providers.gradleProperty("project.version").getOrElse("0.1.0-SNAPSHOT")
val frameworkName = providers.gradleProperty("project.frameworkName").getOrElse("Unnamed")
val projectNamespace = providers.gradleProperty("project.namespace").getOrElse("io.github.kotlinmania")
val kotlinVersion = libs.versions.kotlin.get()

fun csvProperty(name: String): Set<String> =
    providers
        .gradleProperty(name)
        .map { value ->
            value
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }.getOrElse(emptySet())

val intellijCoroutinesVersion =
    libs.versions.intellijCoroutines.get()

// KGP runs Swift Export in an isolated worker whose classpath is
// `swiftExportClasspath`. Adding a dependency disables KGP's default
// dependency population, so keep the default embeddable runner explicit too.
val projectDependencyHandler = project.dependencies
configurations.configureEach {
    if (name == "swiftExportClasspath") {
        dependencies.add(projectDependencyHandler.create("org.jetbrains.kotlin:swift-export-embeddable:$kotlinVersion"))
        dependencies.add(
            projectDependencyHandler.create(
                "org.jetbrains.intellij.deps.kotlinx:kotlinx-coroutines-core-jvm:$intellijCoroutinesVersion",
            ),
        )
    }
}

// Opt-ins shared across Kotlin targets.
// Read from gradle.properties so the android project compiles commonMain with
// the same opt-ins.
val commonOptIns = csvProperty("project.kotlin.optIns")

// Gap #9b: KGP-generated bridge boilerplate and KotlinCoroutineSupport runtime
// produce warnings (unchecked casts, unused expressions, opt-in requirements)
// that cannot be fixed in source — they are regenerated every build.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name.startsWith("compileSwiftExport")) {
        compilerOptions.allWarningsAsErrors.set(false)
    }
}

val jvmToolchainVersion =
    libs.versions.jvmToolchain
        .get()
        .toInt()

// ============================================================================
// kotlin { … }
// ----------------------------------------------------------------------------
// Workspace product policy retires every native 32-bit target, including
//   watchosArm32 / watchosArm64 (arm64_32), androidNativeArm32 / androidNativeX86,
//   linuxArm32Hfp, and mingwX86. Do not re-add their declarations or CI tasks.
// Apple-specific x64 targets are also retired: iosX64, macosX64, tvosX64,
//   watchosX64. Linux/Windows x64 and Android native x64 remain supported.
// Every retained target is built unconditionally, with no opt-in build gates.
// ============================================================================
kotlin {
    jvmToolchain(jvmToolchainVersion)

    applyDefaultHierarchyTemplate()

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        allWarningsAsErrors.set(true)
        optIn.addAll(commonOptIns)
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xsuppress-version-warnings")
    }

    val xcf = XCFramework(frameworkName)
    val frameworkBundleId = projectNamespace

    // Local helper: attach this target's framework to the XCFramework.
    fun KotlinNativeTarget.addToXcf(static: Boolean = false) {
        binaries.framework {
            baseName = frameworkName
            if (static) isStatic = true
            xcf.add(this)
            binaryOption("bundleId", frameworkBundleId)
        }
    }

    // Apple — Tier 1/2 targets
    macosArm64 {
        addToXcf()
    }
    iosArm64 {
        addToXcf(static = true)
    }
    iosSimulatorArm64 {
        addToXcf(static = true)
    }
    tvosArm64 {
        addToXcf()
    }
    tvosSimulatorArm64 {
        addToXcf()
    }
    // watchosArm64 (WatchOS 32 / arm64_32): retired by workspace policy (§5.5.1). WatchOS 32 is not supported.
    watchosDeviceArm64 {
        addToXcf()
    }
    watchosSimulatorArm64 {
        addToXcf()
    }

    // Other native — Tier 1/2
    linuxX64()
    linuxArm64()
    mingwX64()

    // Android NDK — 64-bit only (32-bit retired §5.5.3, 2026-06-25).
    androidNativeArm64()
    androidNativeX64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.staticLib {
            baseName = frameworkName
        }
        binaries.sharedLib {
            baseName = frameworkName
        }
    }

    // Web
    js {
        browser()
        nodejs()
    }

    // wasmJs is Stable as of Kotlin 2.2; @OptIn may be removable — verify before dropping on wasmWasi.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Swift Export bridge — Experimental per Kotlin 2.4.0 release notes.
    // KGP 2.4.0 does not expose a public opt-in annotation; warnings (if any)
    // arrive via KotlinToolingDiagnostics, not @RequiresOptIn.
    swiftExport {
        moduleName = frameworkName
        flattenPackage = projectNamespace
        @OptIn(org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl::class)
        configure {
            settings.put("enableCoroutinesSupport", "true")
        }
    }

    // JVM — jvmTarget derived from the same toolchain property so they can't drift.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jvmToolchainVersion.toString()))
        }
    }

    // Dependencies are declared per source set from gradle/libs.versions.toml.
    // A platform or shared source set (jsMain, appleMain, nativeMain, ...) gets
    // its own block here when it needs dependencies of its own.
    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.common.main)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.bundles.common.test)
        }
    }
}

// ============================================================================
// Test logging
// ============================================================================
tasks.withType<AbstractTestTask>().configureEach {
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
        showStandardStreams = true
    }
}

// ============================================================================
// Static analysis: Detekt + Ktlint
// ============================================================================
detekt {
    buildUponDefaultConfig.set(true)
    allRules.set(false)
    autoCorrect.set(false)
    source.setFrom(files("src"))
    config.setFrom(files("detekt.yml"))
    parallel.set(true)
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true)
        checkstyle.required.set(false)
    }
}

ktlint {
    version.set(libs.versions.ktlintEngine.get())
    debug.set(false)
    verbose.set(false)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
    }
    filter {
        exclude("**/build/**")
        include("**/src/**/kotlin/**")
    }
}

// check is static analysis. Tests are not part of build: they run through test
// and test<Platform>.
tasks.named("check") {
    dependsOn(tasks.withType<dev.detekt.gradle.Detekt>())
    dependsOn(tasks.named("ktlintCheck"))
}

// The Kotlin and Android plugins attach every test run to check while they
// finish configuring the project. Remove those edges once evaluation is done,
// so check stays static analysis and build runs no tests; test runs them.
fun Task.isTestRun(): Boolean = this is AbstractTestTask || name == "allTests" || name.startsWith("test")

afterEvaluate {
    tasks.named("check") {
        setDependsOn(
            dependsOn.filterNot { dependency ->
                when (dependency) {
                    is Task -> dependency.isTestRun()
                    is TaskProvider<*> -> dependency.get().isTestRun()
                    is String -> tasks.findByName(dependency.substringAfterLast(':'))?.isTestRun() == true
                    else -> false
                }
            },
        )
    }
}

// ============================================================================
// JS / Wasm toolchain pins
// ============================================================================
val nodeVersion = libs.versions.node.get()
val wasmNodeVersion = nodeVersion
val yarnVersion = libs.versions.yarn.get()
val wasmYarnVersion = yarnVersion

// webpack is pinned in kotlin-js-store/package.json — the single source of truth
// that Dependabot updates natively. Gradle reads the version from there so the
// yarn resolution and the NodeJsRootExtension pin always track the checked-in
// store; a Dependabot bump of package.json/yarn.lock is honored rather than
// overridden. (These two values previously lived in gradle.properties, which
// Dependabot cannot see, so a bump there would silently revert the build.)
@Suppress("UNCHECKED_CAST")
val webpackVersion: String =
    (JsonSlurper().parse(rootProject.file("kotlin-js-store/package.json")) as Map<String, Any>)
        .let { it["dependencies"] as Map<String, Any> }["webpack"] as String

rootProject.extensions.configure<NodeJsEnvSpec>("kotlinNodeJsSpec") { version.set(nodeVersion) }
rootProject.extensions.configure<WasmNodeJsEnvSpec>("kotlinWasmNodeJsSpec") { version.set(wasmNodeVersion) }
rootProject.extensions.configure<YarnRootEnvSpec>("kotlinYarnSpec") {
    version.set(yarnVersion)
    yarnLockMismatchReport.set(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.WARNING)
    yarnLockAutoReplace.set(true)
}
rootProject.extensions.configure<WasmYarnRootEnvSpec>("kotlinWasmYarnSpec") {
    version.set(wasmYarnVersion)
    yarnLockMismatchReport.set(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.WARNING)
    yarnLockAutoReplace.set(true)
}

rootProject.extensions.configure<YarnRootExtension>("kotlinYarn") {
    providers
        .gradlePropertiesPrefixedBy("yarn.resolution.")
        .getOrElse(emptyMap())
        .forEach { (key, ver) ->
            val pkg = key.removePrefix("yarn.resolution.")
            resolution(pkg, ver)
            resolution("**/$pkg", ver)
        }
    // webpack resolution sourced from kotlin-js-store/package.json (see above)
    // rather than a yarn.resolution.webpack property, so it can never override a
    // Dependabot bump of the store.
    resolution("webpack", webpackVersion)
    resolution("**/webpack", webpackVersion)
}

val patchedKarmaWebpackPackage =
    rootProject.layout.projectDirectory
        .dir("gradle/npm/karma-webpack")
        .asFile.absolutePath
        .replace("\\", "/")

// TODO: NodeJsRootExtension.versions.* is deprecated and will be removed when the spec-based
//       NodeJsEnvSpec API gains equivalent properties. Track KGP release notes before removing.
rootProject.extensions.configure<NodeJsRootExtension>("kotlinNodeJs") {
    versions.webpack.version = webpackVersion
    versions.webpackCli.version = libs.versions.webpackCli.get()
    versions.karma.version = libs.versions.karma.get()
    versions.karmaWebpack.version = "file:$patchedKarmaWebpackPackage"
    versions.mocha.version = libs.versions.mocha.get()
    versions.kotlinWebHelpers.version = libs.versions.kotlinWebHelpers.get()
}

// Make kotlinUpgradeYarnLock and kotlinWasmUpgradeYarnLock dependencies in the build process
// for KotlinJS and other JavaScript/WASM targets so that yarn.lock is always upgraded automatically.
val jsTasksNeedingYarnLock =
    setOf(
        "compileKotlinJs",
        "compileTestKotlinJs",
        "jsProcessResources",
        "jsTestProcessResources",
        "jsNodeTest",
        "jsBrowserTest",
        "kotlinStoreYarnLock",
    )

tasks
    .matching { it.name in jsTasksNeedingYarnLock }
    .configureEach {
        dependsOn("kotlinUpgradeYarnLock")
    }

val wasmTasksNeedingYarnLock =
    setOf(
        "compileKotlinWasmJs",
        "compileTestKotlinWasmJs",
        "wasmJsProcessResources",
        "wasmJsTestProcessResources",
        "wasmJsNodeTest",
        "wasmJsBrowserTest",
        "compileKotlinWasmWasi",
        "compileTestKotlinWasmWasi",
        "wasmWasiProcessResources",
        "wasmWasiTestProcessResources",
        "wasmWasiNodeTest",
        "kotlinWasmStoreYarnLock",
    )

tasks
    .matching { it.name in wasmTasksNeedingYarnLock }
    .configureEach {
        dependsOn("kotlinWasmUpgradeYarnLock")
    }

// ============================================================================
// Maven Central publishing — Central Portal, first-party + bespoke upload
// ----------------------------------------------------------------------------
// OSSRH was sunset 2025-06-30; the Central Portal is the only path. Sonatype
// ships no first-party Gradle plugin, so we use Gradle's own maven-publish +
// signing (KGP populates the KMP publications) and upload the deployment
// bundle to the Portal API ourselves.
//
// Flow: publish all KMP publications into a local staging Maven layout ->
// zip it -> POST the zip to the Portal upload endpoint with a Bearer token.
// ============================================================================
val publishProjectName = providers.gradleProperty("project.name").getOrElse("unnamed-project")

// Central requires a Javadoc jar per publication; KMP produces none, so attach
// an empty one to every Maven publication.
val emptyJavadocJar =
    tasks.register<Jar>("emptyJavadocJar") {
        description = ""
        archiveClassifier.set("javadoc")
    }

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifact(emptyJavadocJar)
        pom {
            name.set(publishProjectName)
            description.set(providers.gradleProperty("project.pom.description").getOrElse(""))
            inceptionYear.set("2026")
            url.set("https://github.com/KotlinMania/$publishProjectName")
            licenses {
                license {
                    name.set(providers.gradleProperty("project.pom.licenseName").getOrElse("MIT"))
                    url.set(
                        providers
                            .gradleProperty("project.pom.licenseUrl")
                            .getOrElse("https://opensource.org/licenses/MIT"),
                    )
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("sydneyrenee")
                    name.set("Sydney Renee")
                    email.set("sydney@solace.ofharmony.ai")
                    url.set("https://github.com/sydneyrenee")
                }
            }
            scm {
                url.set("https://github.com/KotlinMania/$publishProjectName")
                connection.set("scm:git:git://github.com/KotlinMania/$publishProjectName.git")
                developerConnection.set("scm:git:ssh://github.com/KotlinMania/$publishProjectName.git")
            }
        }
    }

    // Stage into a local Maven layout that becomes the Portal deployment bundle.
    // maven-publish auto-generates the md5/sha1/sha256/sha512 checksums Central
    // requires; signing (below) adds the .asc signatures.
    repositories {
        maven {
            name = "centralPortalStaging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
    val signingKeyId = providers.gradleProperty("signingInMemoryKeyId").orNull
    val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull
    val signingEnabled = project.findProperty("RELEASE_SIGNING_ENABLED") != "false" && signingKey != null
    if (signingEnabled) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}

val centralPortalPublishTasks =
    tasks.withType<PublishToMavenRepository>().matching {
        it.name.endsWith("ToCentralPortalStagingRepository")
    }

centralPortalPublishTasks.configureEach {
    dependsOn(tasks.withType<Sign>())
}

// The root module lists every platform's variants. Android builds and publishes
// in the android project, so its variants are added here, each pointing at the
// Android module, the same way the Kotlin plugin points at every other
// platform's module.
val androidModuleFile =
    project(":android").layout.buildDirectory.file("publications/android/module.json")

tasks.named<GenerateModuleMetadata>("generateMetadataFileForKotlinMultiplatformPublication") {
    dependsOn(":android:generateMetadataFileForAndroidPublication")
    inputs.file(androidModuleFile)
    val androidModule = "$publishProjectName-android"
    val libraryGroup = project.group.toString()
    val libraryVersion = project.version.toString()
    doLast {
        val rootModuleFile = outputFile.get().asFile
        val rootModule = JsonSlurper().parse(rootModuleFile) as Map<*, *>
        val android = JsonSlurper().parse(androidModuleFile.get().asFile) as Map<*, *>
        val androidVariants =
            (android["variants"] as List<*>).map { variant ->
                variant as Map<*, *>
                mapOf(
                    "name" to variant["name"],
                    "attributes" to variant["attributes"],
                    "available-at" to
                        mapOf(
                            "url" to "../../$androidModule/$libraryVersion/$androidModule-$libraryVersion.module",
                            "group" to libraryGroup,
                            "module" to androidModule,
                            "version" to libraryVersion,
                        ),
                )
            }
        val variants = (rootModule["variants"] as List<*>) + androidVariants
        val rewritten = rootModule.entries.associate { (it.key as String) to it.value } + ("variants" to variants)
        rootModuleFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(rewritten)))
    }
}

// Zip the staged Maven layout into a single Central Portal deployment bundle.
val centralPortalBundle =
    tasks.register<Zip>("centralPortalBundle") {
        group = "publishing"
        description = "Bundles the staged Maven artifacts into a Central Portal deployment zip."
        dependsOn(centralPortalPublishTasks)
        dependsOn(":android:publishAndroidPublicationToCentralPortalStagingRepository")
        from(layout.buildDirectory.dir("staging-deploy"))
        archiveFileName.set("$publishProjectName-$version-bundle.zip")
        destinationDirectory.set(layout.buildDirectory.dir("central-portal"))
    }

// Upload the bundle to the Sonatype Central Portal Publisher API.
// publishingType: USER_MANAGED (default, safe — validates then waits for a
// manual release in the Portal UI) or AUTOMATIC (publishes after validation).
val publishToCentralPortal =
    tasks.register("publishToCentralPortal") {
        group = "publishing"
        description = "Uploads the deployment bundle to the Sonatype Central Portal."
        dependsOn(centralPortalBundle)
        doLast {
            val user =
                providers.gradleProperty("mavenCentralUsername").orNull
                    ?: error("mavenCentralUsername is required to publish to the Central Portal.")
            val password =
                providers.gradleProperty("mavenCentralPassword").orNull
                    ?: error("mavenCentralPassword is required to publish to the Central Portal.")
            val publishingType = providers.gradleProperty("centralPublishingType").getOrElse("USER_MANAGED")
            val token = Base64.getEncoder().encodeToString("$user:$password".toByteArray(Charsets.UTF_8))

            val bundle =
                centralPortalBundle
                    .get()
                    .archiveFile
                    .get()
                    .asFile
            require(bundle.exists()) { "Deployment bundle not found: $bundle" }

            val boundary = "CentralPortalBoundary" + UUID.randomUUID().toString().replace("-", "")
            val crlf = "\r\n"
            val preamble =
                (
                    "--$boundary$crlf" +
                        "Content-Disposition: form-data; name=\"bundle\"; filename=\"${bundle.name}\"$crlf" +
                        "Content-Type: application/octet-stream$crlf$crlf"
                ).toByteArray(Charsets.UTF_8)
            val epilogue = "$crlf--$boundary--$crlf".toByteArray(Charsets.UTF_8)
            val body = preamble + bundle.readBytes() + epilogue

            val deploymentName = "$publishProjectName-$version"
            val uploadUri =
                URI(
                    "https://central.sonatype.com/api/v1/publisher/upload" +
                        "?name=$deploymentName&publishingType=$publishingType",
                )
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(uploadUri)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "multipart/form-data; boundary=$boundary")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build()

            val client = HttpClient.newHttpClient()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                error("Central Portal upload failed: HTTP ${response.statusCode()} — ${response.body()}")
            }
            val deploymentId = response.body().trim()
            logger.lifecycle(
                "Central Portal upload accepted (deployment id: $deploymentId). " +
                    "publishingType=$publishingType.",
            )
            val automaticPublishing = publishingType.equals("AUTOMATIC", ignoreCase = true)
            val terminalStates =
                if (automaticPublishing) {
                    setOf("PUBLISHED")
                } else {
                    setOf("VALIDATED", "PUBLISHED")
                }
            val statusUri = URI("https://central.sonatype.com/api/v1/publisher/status?id=$deploymentId")
            val statusAttempts =
                providers.gradleProperty("centralPublishStatusAttempts").map(String::toInt).getOrElse(120)
            val statusDelayMillis =
                providers.gradleProperty("centralPublishStatusDelayMillis").map(String::toLong).getOrElse(10_000L)
            repeat(statusAttempts) { attempt ->
                val statusRequest =
                    HttpRequest
                        .newBuilder()
                        .uri(statusUri)
                        .header("Authorization", "Bearer $token")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build()
                val statusResponse = client.send(statusRequest, HttpResponse.BodyHandlers.ofString())
                if (statusResponse.statusCode() !in 200..299) {
                    error("Central Portal status check failed: HTTP ${statusResponse.statusCode()} — ${statusResponse.body()}")
                }
                val statusBody = JsonSlurper().parseText(statusResponse.body()) as Map<*, *>
                val deploymentState =
                    statusBody["deploymentState"]?.toString()
                        ?: error("Central Portal status response did not contain deploymentState: ${statusResponse.body()}")
                when (deploymentState) {
                    "FAILED" -> {
                        error("Central Portal deployment failed: ${statusBody["errors"] ?: statusResponse.body()}")
                    }

                    in terminalStates -> {
                        logger.lifecycle("Central Portal deployment $deploymentId reached $deploymentState.")
                        return@doLast
                    }
                }
                logger.lifecycle(
                    "Central Portal deployment $deploymentId is $deploymentState " +
                        "(${attempt + 1}/$statusAttempts).",
                )
                if (attempt + 1 < statusAttempts) {
                    Thread.sleep(statusDelayMillis)
                }
            }
            error(
                "Central Portal deployment $deploymentId did not reach " +
                    "${terminalStates.joinToString("/")} after $statusAttempts checks.",
            )
        }
    }

// ============================================================================
// Tasks
// ============================================================================

// Runs every test this host can execute: commonTest on every target with a test
// runner, plus the Swift Export parity test. The android project registers its
// own test task, so ./gradlew test runs the Android tests as well.
tasks.register("test") {
    group = "verification"
    description = "Runs every test this host can execute, on every platform."
}

// Patch generated SPM Package.swift to include minimum macOS platform for Swift Concurrency
tasks.matching { it.name.contains("GenerateSPMPackage") }.configureEach {
    doLast {
        val spmDir =
            layout.buildDirectory
                .dir("SPMPackage")
                .orNull
                ?.asFile
        if (spmDir != null && spmDir.exists()) {
            spmDir.walkTopDown().filter { it.name == "Package.swift" }.forEach { file ->
                var text = file.readText()
                if (text.contains("swift-tools-version: 6.0")) {
                    text = text.replace("swift-tools-version: 6.0", "swift-tools-version: 5.9")
                }
                if (!text.contains("platforms:")) {
                    text =
                        text.replaceFirst(
                            Regex("""(let package = Package\s*\(\s*name:\s*"[^"]*",)"""),
                            "$1\n    platforms: [.macOS(\"15.0\")],",
                        )
                } else if (text.contains(".macOS(.v15)") || text.contains(".macOS(.v14)")) {
                    text = text.replace(".macOS(.v15)", ".macOS(\"15.0\")").replace(".macOS(.v14)", ".macOS(\"15.0\")")
                }
                file.writeText(text)
            }
            spmDir.walkTopDown().filter { it.name == "OrgJetbrainsKotlinxKotlinxSerializationCore.swift" }.forEach { file ->
                val text = file.readText()
                val cleaned =
                    text.replace(
                        Regex("""@_spi\([^)]+\)\s+public func (?:decodeSequentially|shouldEncodeElementDefault|encodeNotNullMark)\([^)]*\)(?:\s*->\s*[^\n{]+)?\s*\{\s*fatalError\([^)]+\)\s*\}"""),
                        "",
                    )
                if (cleaned != text) {
                    file.writeText(cleaned)
                }
            }
        }
    }
}

// Swift Export smoke test — produces the SPM package via embedSwiftExportForXcode
// (spawned with the Xcode-style env it requires) and runs `swift test` against it,
// so Swift Export breakage surfaces locally, not only in the swift.yml CI job.
// Pattern mirrors kasuari-kotlin. This task is part of the build contract and
// must fail rather than skip when the required toolchain is unavailable.
tasks.register("swiftExportSmokeTest") {
    group = "verification"
    description = "Builds the Swift Export SPM package and runs swift test against it."
    outputs.upToDateWhen { false }

    doLast {
        val execOperations = serviceOf<ExecOperations>()
        val swiftBuildFile =
            layout.buildDirectory
                .dir("swift-test")
                .get()
                .asFile
        if (swiftBuildFile.exists()) {
            swiftBuildFile.deleteRecursively()
        }
        swiftBuildFile.mkdirs()
        val swiftBuildDir = swiftBuildFile.absolutePath
        execOperations
            .exec {
                workingDir = projectDir
                commandLine(
                    "./gradlew",
                    "embedSwiftExportForXcode",
                    "-Dorg.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g",
                    "--no-configuration-cache",
                    "--no-daemon",
                    "--console=plain",
                )
                environment(
                    mapOf(
                        "BUILT_PRODUCTS_DIR" to swiftBuildDir,
                        "TARGET_BUILD_DIR" to swiftBuildDir,
                        "SDK_NAME" to "macosx",
                        "CONFIGURATION" to "Debug",
                        "ARCHS" to "arm64",
                        "FRAMEWORKS_FOLDER_PATH" to "Frameworks",
                        "MACOSX_DEPLOYMENT_TARGET" to "15.0",
                        "DEPLOYMENT_TARGET_SETTING_NAME" to "MACOSX_DEPLOYMENT_TARGET",
                    ),
                )
            }.assertNormalExitValue()

        execOperations
            .exec {
                workingDir = layout.projectDirectory.dir("swift-test-harness").asFile
                commandLine("swift", "package", "reset")
            }.assertNormalExitValue()

        execOperations
            .exec {
                workingDir = layout.projectDirectory.dir("swift-test-harness").asFile
                commandLine("swift", "test")
            }.assertNormalExitValue()
    }
}

// ============================================================================
// IDE dependency views
// ----------------------------------------------------------------------------
// IntelliJ's Gradle tool window lists dependencies per Java source set: each
// one's compile and runtime classpath. The Kotlin plugin registers Java source
// sets only for jvm(). Every Kotlin source set gets a view here, so the tree
// shows dependencies by platform and by every shared source set (commonMain,
// nativeMain, appleMain, iosMain, webMain, browserMain, posixMain, ...):
//
//   <sourceSet>DependenciesCompileClasspath  what the source set compiles against
//   <sourceSet>DependenciesRuntimeClasspath  what it runs with
//
// A platform source set mirrors its compilation's classpaths; a shared source
// set mirrors its metadata compilation. Each view extends the classpath it
// mirrors and carries its attributes, so it resolves exactly the same graph.
// ============================================================================
fun <T : Any> copyAttribute(
    key: Attribute<T>,
    from: AttributeContainer,
    to: AttributeContainer,
) {
    from.getAttribute(key)?.let { to.attribute(key, it) }
}

fun Configuration.mirror(source: Configuration) {
    extendsFrom(source)
    source.attributes.keySet().forEach { copyAttribute(it, source.attributes, attributes) }
}

val javaSourceSets = extensions.getByType<JavaPluginExtension>().sourceSets

fun registerDependencyView(
    sourceSetName: String,
    compileClasspathName: String,
    runtimeClasspathName: String,
) {
    val view = javaSourceSets.maybeCreate("${sourceSetName}Dependencies")
    configurations.named(view.compileClasspathConfigurationName) { mirror(configurations.getByName(compileClasspathName)) }
    configurations.named(view.runtimeClasspathConfigurationName) { mirror(configurations.getByName(runtimeClasspathName)) }
}

kotlin.targets.configureEach {
    compilations.configureEach {
        val compileClasspathName = compileDependencyConfigurationName
        registerDependencyView(
            defaultSourceSet.name,
            compileClasspathName,
            runtimeDependencyConfigurationName ?: compileClasspathName,
        )
    }
}

// ============================================================================
// Build lifecycle
// ----------------------------------------------------------------------------
// The build owns its task graph; CI and the IDE only invoke it. Every declared
// target joins a platform by family, and each platform gets four tasks:
//
//   compile<Platform>   every main and test compilation of the platform
//   assemble<Platform>  compile<Platform> plus jars and klibs, and every native
//                       binary: static and shared libraries, frameworks, test
//                       executables
//   build<Platform>     assemble<Platform>; builds, never runs tests
//   test<Platform>      every test of the platform this host can run; runs the
//                       host cannot execute (Linux on macOS, Windows elsewhere)
//                       are skipped by the Kotlin plugin
//
// assemble depends on every assemble<Platform>, so build compiles and links
// every target; test depends on every test<Platform>. Adding a target to the
// kotlin { } block adds it to these tasks. Android builds in the android
// project.
// ============================================================================
val retiredTargetNames =
    setOf(
        "androidNativeArm32",
        "androidNativeX86",
        "iosX64",
        "linuxArm32Hfp",
        "macosX64",
        "mingwX86",
        "tvosX64",
        "watchosArm32",
        "watchosArm64",
        "watchosX64",
    )

kotlin.targets.configureEach {
    if (name in retiredTargetNames) {
        throw GradleException("Target '$name' is retired by workspace policy (§5.5) and must not be declared.")
    }
}

fun KotlinTarget.platformName(): String? =
    when (platformType) {
        KotlinPlatformType.common -> null
        KotlinPlatformType.jvm -> "Jvm"
        KotlinPlatformType.js -> "Js"
        KotlinPlatformType.wasm -> "Wasm"
        KotlinPlatformType.androidJvm ->
            throw GradleException("Target '$name' belongs in the android project, not the root build.")
        KotlinPlatformType.native ->
            when ((this as KotlinNativeTarget).konanTarget.family) {
                Family.ANDROID -> "AndroidNative"
                Family.IOS -> "Ios"
                Family.OSX -> "Macos"
                Family.TVOS -> "Tvos"
                Family.WATCHOS -> "Watchos"
                Family.LINUX -> "Linux"
                Family.MINGW -> "Windows"
                else -> throw GradleException("Target '$name' has no platform in the build lifecycle.")
            }
    }

val platformNames = listOf("Jvm", "AndroidNative", "Ios", "Macos", "Tvos", "Watchos", "Linux", "Windows", "Js", "Wasm")

// Outputs a platform builds beyond its targets' own: the JS and Wasm lockfile
// stores, and the Apple XCFramework, which spans every Apple platform.
val platformExtraOutputs =
    mapOf(
        "Js" to listOf("kotlinUpgradeYarnLock", "kotlinStoreYarnLock"),
        "Wasm" to listOf("kotlinWasmUpgradeYarnLock", "kotlinWasmStoreYarnLock"),
        "Ios" to listOf("assemble${frameworkName}XCFramework"),
    )

// Tests a platform runs beyond its targets' test runs: Swift Export builds the
// macOS slice and runs swift test against it.
val platformExtraTests = mapOf("Macos" to listOf("swiftExportSmokeTest"))

fun platformTargets(platform: String) = kotlin.targets.matching { it.platformName() == platform }

for (platform in platformNames) {
    val compilePlatform =
        tasks.register("compile$platform") {
            group = "build"
            description = "Compiles every main and test compilation of the $platform targets."
            platformTargets(platform).configureEach {
                compilations.configureEach {
                    dependsOn(compileTaskProvider)
                    dependsOn(compileAllTaskName)
                }
            }
        }
    val assemblePlatform =
        tasks.register("assemble$platform") {
            group = "build"
            description = "Builds every artifact and binary of the $platform targets."
            dependsOn(compilePlatform)
            dependsOn(platformExtraOutputs[platform].orEmpty())
            platformTargets(platform).configureEach {
                dependsOn(artifactsTaskName)
                if (this is KotlinNativeTarget) {
                    binaries.configureEach { dependsOn(linkTaskProvider) }
                }
            }
        }
    val testPlatform =
        tasks.register("test$platform") {
            group = "verification"
            description = "Runs every test of the $platform targets that this host can execute."
            dependsOn(platformExtraTests[platform].orEmpty())
            platformTargets(platform).configureEach {
                if (this is KotlinTargetWithTests<*, *>) {
                    testRuns.configureEach {
                        if (this is ExecutionTaskHolder<*>) dependsOn(executionTask)
                    }
                }
            }
        }
    tasks.register("build$platform") {
        group = "build"
        description = "Builds every artifact and binary of the $platform targets. Runs no tests."
        dependsOn(assemblePlatform)
    }
    tasks.named("test") { dependsOn(testPlatform) }
}

tasks.named("assemble") { dependsOn(platformNames.map { "assemble$it" }) }
