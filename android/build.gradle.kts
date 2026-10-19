import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

// ============================================================================
// Android build
// ----------------------------------------------------------------------------
// The Android target lives in this project so the Android Gradle Plugin is
// applied here and nowhere else. AGP checks dependency resolution on every
// configuration of the project it is applied to, and in the root project that
// check fires on the Kotlin/JS and Kotlin/Wasm npm configurations, which the
// Kotlin plugin resolves while it builds the task graph. The root build loads
// AGP with apply false so both plugins share one classloader.
//
// This project compiles the library's commonMain and commonTest sources from
// the root src/ tree for Android, with the same catalog bundles and compiler
// settings as the root build, and runs them as Android host and device tests.
// ============================================================================
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("org.jlleitschuh.gradle.ktlint")
    `java-base`
    `maven-publish`
    signing
}

group = providers.gradleProperty("project.group").getOrElse("io.github.kotlinmania")
version = providers.gradleProperty("project.version").getOrElse("0.1.0-SNAPSHOT")
val projectNamespace = providers.gradleProperty("project.namespace").getOrElse("io.github.kotlinmania")
val jvmToolchainVersion =
    libs.versions.jvmToolchain
        .get()
        .toInt()
val rootSources = rootProject.layout.projectDirectory.dir("src")
val managedDeviceName = "pixel6"

val commonOptIns =
    providers
        .gradleProperty("project.kotlin.optIns")
        .get()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

// ============================================================================
// Android SDK installer
// ----------------------------------------------------------------------------
// The Android Gradle Plugin resolves the SDK location at configuration time,
// so the SDK must already be on disk before the `kotlin { android { ... } }`
// block evaluates. The installer is idempotent — a .install-complete marker
// short-circuits the download on every subsequent invocation, so warm runs
// pay only a directory-existence check. CI runners pay a one-time cold cost
// the first time they touch the project.
// ============================================================================

val androidCommandLineToolsRevision = libs.versions.androidCommandLineTools.get()
val projectCompileSdk = libs.versions.androidCompileSdk.get()
val projectAndroidBuildTools = libs.versions.androidBuildTools.get()
val osName = providers.systemProperty("os.name").get().lowercase()
val isWindowsHost = "windows" in osName
val isMacHost = "mac" in osName
val androidSdkOsName =
    when {
        isWindowsHost -> "win"
        isMacHost -> "mac"
        "linux" in osName -> "linux"
        else -> throw GradleException("Unsupported Android SDK setup OS: ${providers.systemProperty("os.name").get()}")
    }
val projectAndroidSdkDir =
    rootProject.layout.projectDirectory
        .dir(".android-sdk")
        .asFile
val androidSdkManager =
    projectAndroidSdkDir.resolve(
        if (isWindowsHost) {
            "cmdline-tools/latest/bin/sdkmanager.bat"
        } else {
            "cmdline-tools/latest/bin/sdkmanager"
        },
    )
val androidSdkInstallMarker = projectAndroidSdkDir.resolve(".install-complete")
val requiredAndroidSdkPackageDirs =
    listOf(
        projectAndroidSdkDir.resolve("platform-tools"),
        projectAndroidSdkDir.resolve("platforms/android-$projectCompileSdk"),
        projectAndroidSdkDir.resolve("build-tools/$projectAndroidBuildTools"),
    )

fun writeAndroidLocalProperties() {
    projectAndroidSdkDir.mkdirs()
    val sdkDirPropertyValue = projectAndroidSdkDir.absolutePath.replace("\\", "/")
    rootProject.layout.projectDirectory
        .file("local.properties")
        .asFile
        .writeText("sdk.dir=$sdkDirPropertyValue\n")
}

fun isProjectAndroidSdkInstalled(): Boolean =
    androidSdkInstallMarker.exists() &&
        androidSdkManager.exists() &&
        requiredAndroidSdkPackageDirs.all { it.exists() }

fun sdkManagerCommand(vararg args: String): List<String> =
    if (isWindowsHost) {
        listOf("cmd", "/c", androidSdkManager.absolutePath) + args
    } else {
        listOf(androidSdkManager.absolutePath) + args
    }

fun downloadAndroidCommandLineTools() {
    val zipName = "commandlinetools-$androidSdkOsName-${androidCommandLineToolsRevision}_latest.zip"
    val url = "https://dl.google.com/android/repository/$zipName"
    val tmpDir = projectAndroidSdkDir.resolve(".tmp/commandline-tools")
    val zipFile = tmpDir.resolve(zipName)
    val latestDir = projectAndroidSdkDir.resolve("cmdline-tools/latest")
    println("setup-android-sdk: downloading $url")
    tmpDir.deleteRecursively()
    tmpDir.mkdirs()
    try {
        URI(url).toURL().openStream().use { input ->
            Files.copy(input, zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        latestDir.deleteRecursively()
        latestDir.mkdirs()
        val canonicalLatestDir = latestDir.canonicalFile.toPath()
        ZipInputStream(zipFile.inputStream().buffered()).use { zipInput ->
            generateSequence { zipInput.nextEntry }.forEach { entry ->
                val relativeName = entry.name.removePrefix("cmdline-tools/").trimStart('/')
                if (relativeName.isNotEmpty()) {
                    val target = latestDir.resolve(relativeName).canonicalFile
                    if (!target.toPath().startsWith(canonicalLatestDir)) {
                        throw GradleException("Refusing to extract Android SDK entry outside $latestDir: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile.mkdirs()
                        Files.copy(zipInput, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        if (!isWindowsHost && relativeName.startsWith("bin/")) target.setExecutable(true)
                    }
                }
                zipInput.closeEntry()
            }
        }
        if (!isWindowsHost) androidSdkManager.setExecutable(true)
    } finally {
        tmpDir.deleteRecursively()
    }
}

fun installProjectAndroidSdk(execOperations: ExecOperations) {
    if (isProjectAndroidSdkInstalled()) {
        writeAndroidLocalProperties()
        println("setup-android-sdk: SDK already installed at $projectAndroidSdkDir")
        return
    }
    if (!androidSdkManager.exists()) downloadAndroidCommandLineTools()
    println("setup-android-sdk: accepting licenses")
    val licenseAnswers = "y\n".repeat(200).toByteArray(Charsets.UTF_8)
    val licenseResult =
        execOperations.exec {
            commandLine(sdkManagerCommand("--sdk_root=${projectAndroidSdkDir.absolutePath}", "--licenses"))
            standardInput = ByteArrayInputStream(licenseAnswers)
            isIgnoreExitValue = true
        }
    if (licenseResult.exitValue != 0) {
        throw GradleException("Android SDK license acceptance failed with exit code ${licenseResult.exitValue}")
    }
    println(
        "setup-android-sdk: installing platform-tools, android-$projectCompileSdk, build-tools;$projectAndroidBuildTools",
    )
    val installLog = projectAndroidSdkDir.resolve("sdkmanager-install.log")
    installLog.parentFile.mkdirs()
    installLog.outputStream().use { output ->
        val installResult =
            execOperations.exec {
                commandLine(
                    sdkManagerCommand(
                        "--sdk_root=${projectAndroidSdkDir.absolutePath}",
                        "platform-tools",
                        "platforms;android-$projectCompileSdk",
                        "build-tools;$projectAndroidBuildTools",
                    ),
                )
                standardOutput = output
                errorOutput = output
                isIgnoreExitValue = true
            }
        if (installResult.exitValue != 0) {
            throw GradleException(
                "Android SDK package install failed with exit code ${installResult.exitValue}. " +
                    "Install log:\n${installLog.readText()}",
            )
        }
    }
    writeAndroidLocalProperties()
    androidSdkInstallMarker.writeText("")
    println("setup-android-sdk: done; SDK at $projectAndroidSdkDir")
}

// ----------------------------------------------------------------------------
// Android SDK setup is gated to follow the requested task. It must never run for
// non-Android invocations (jsTest, jvmTest, swiftExportSmokeTest, native /
// androidNative links) -- an unconditional install here is what made the SDK
// download appear on every machine and target.
//
// `writeAndroidLocalProperties()` always runs: it is cheap, hits no network, and
// only points local.properties at the project-local .android-sdk so AGP can
// resolve `sdk.dir` while the `androidLibrary {}` block evaluates.
//
// The SDK *package* download must happen at configuration time when -- and only
// when -- an Android task is in the requested build. AGP validates the packages
// while determining the dependencies of `compileAndroidMain` (task-graph
// construction, strictly before any task executes), so a plain `dependsOn`
// cannot supply them in time. We detect Android intent from the requested task
// names and install eagerly in that case. androidNative* are Kotlin/Native
// targets and need no Android SDK.
// ----------------------------------------------------------------------------
writeAndroidLocalProperties()

fun requestedTaskWantsAndroid(rawTaskName: String): Boolean {
    val taskName = rawTaskName.substringAfterLast(':')
    if (taskName.contains("AndroidNative")) return false // Kotlin/Native, no SDK
    if (taskName.contains("Android")) return true // direct AGP tasks
    return taskName in setOf("build", "assemble", "check", "test") // lifecycle tasks reach this project
}

if (gradle.startParameter.taskNames.any(::requestedTaskWantsAndroid)) {
    installProjectAndroidSdk(serviceOf())
}

val ensureAndroidSdk =
    tasks.register("ensureAndroidSdk") {
        group = "setup"
        description = "Ensures the project-local Android SDK is installed (idempotent)."
        onlyIf { !isProjectAndroidSdkInstalled() }
        doLast {
            installProjectAndroidSdk(serviceOf())
        }
    }

// Secondary net: order every AGP Android task after the installer (a no-op on
// warm runs). Excludes androidNative* (Kotlin/Native) and the installer itself.
tasks
    .matching { task ->
        val taskName = task.name
        taskName != "ensureAndroidSdk" &&
            taskName.contains("Android") &&
            !taskName.contains("AndroidNative")
    }.configureEach {
        dependsOn(ensureAndroidSdk)
    }

// proc-macro-kotlin's published Android artifact bundles the JetBrains
// annotation classes, which duplicate org.jetbrains:annotations on the
// Android device-test classpath.
configurations.configureEach {
    if (name.contains("androidDeviceTest", ignoreCase = true)) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
}

kotlin {
    jvmToolchain(jvmToolchainVersion)

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        allWarningsAsErrors.set(true)
        optIn.addAll(commonOptIns)
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xsuppress-version-warnings")
    }

    android {
        namespace = projectNamespace
        compileSdk = projectCompileSdk.toInt()
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()
        // Host and device tests both compile commonTest.
        withHostTestBuilder { sourceSetTreeName = "test" }.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            // The managed-device API is incubating in the Android Gradle Plugin;
            // it is the only way the build provisions its own emulator.
            // A Gradle-managed emulator: the build downloads the system image,
            // boots the device headless, runs the tests, and shuts it down.
            managedDevices {
                localDevices.create(managedDeviceName) {
                    device = "Pixel 6"
                    apiLevel = projectCompileSdk.toInt()
                    systemImageSource = "aosp-atd"
                }
            }
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jvmToolchainVersion.toString()))
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(rootSources.dir("commonMain/kotlin"))
            resources.srcDir(rootSources.dir("commonMain/resources"))
            dependencies {
                implementation(libs.bundles.common.main)
            }
        }
        commonTest {
            kotlin.srcDir(rootSources.dir("commonTest/kotlin"))
            resources.srcDir(rootSources.dir("commonTest/resources"))
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.bundles.common.test)
            }
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.android.device.test)
        }
    }
}

ktlint {
    version.set(libs.versions.ktlintEngine.get())
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
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
// Publishing
// ----------------------------------------------------------------------------
// Android ships as <project.name>-android, signed and staged into the root
// build's Central Portal staging directory, so it goes out in the same
// deployment bundle as every other platform. The root module's Gradle metadata
// lists these variants (see the root build), so Android consumers of the
// library's coordinates resolve this AAR. This project's own multiplatform root
// module is not published; the root project's is the library's.
// ============================================================================
val publishProjectName = providers.gradleProperty("project.name").getOrElse("unnamed-project")
val androidArtifactId = "$publishProjectName-android"

val emptyJavadocJar =
    tasks.register<Jar>("emptyJavadocJar") {
        archiveClassifier.set("javadoc")
    }

publishing {
    publications.withType<MavenPublication>().configureEach {
        if (name == "android") artifactId = androidArtifactId
        artifact(emptyJavadocJar)
        pom {
            name.set(androidArtifactId)
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
    repositories {
        maven {
            name = "centralPortalStaging"
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    if (publication.name == "kotlinMultiplatform") enabled = false
}

// The Android module points back at the library's root module, not at this
// project's.
tasks.named<GenerateModuleMetadata>("generateMetadataFileForAndroidPublication") {
    val rootModule = publishProjectName
    val rootGroup = project.group.toString()
    val rootVersion = project.version.toString()
    doLast {
        val moduleFile = outputFile.get().asFile
        val module = JsonSlurper().parse(moduleFile) as Map<*, *>
        val rewritten =
            module.entries.associate { (it.key as String) to it.value } +
                (
                    "component" to
                        mapOf(
                            "url" to "../../$rootModule/$rootVersion/$rootModule-$rootVersion.module",
                            "group" to rootGroup,
                            "module" to rootModule,
                            "version" to rootVersion,
                        )
                )
        moduleFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(rewritten)))
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
// Lifecycle
// ----------------------------------------------------------------------------
// build assembles the AAR and the host and device test packages and runs no
// tests. test runs commonTest as host tests on the JVM and as device tests on
// the managed emulator; connectedAndroidDeviceTest runs the device tests on an
// attached device instead.
// ============================================================================
tasks.named("assemble") {
    dependsOn("assembleAndroidMain", "assembleAndroidHostTest", "assembleAndroidDeviceTest")
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

tasks.register("test") {
    group = "verification"
    description = "Runs the Android host tests and the device tests on the managed emulator."
    dependsOn("testAndroidHostTest", "${managedDeviceName}AndroidDeviceTest")
}

tasks.register("setupAndroidSdk") {
    group = "setup"
    description = "Downloads and configures the project-local Android SDK. (Alias for ensureAndroidSdk)"
    dependsOn("ensureAndroidSdk")
}
