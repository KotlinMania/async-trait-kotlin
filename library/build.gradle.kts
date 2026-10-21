import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import dev.detekt.gradle.Detekt
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.internal.DefaultJavaPluginExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.plugins.signing.Sign
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.WasmBinary
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream
import java.lang.reflect.Proxy
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import java.util.UUID

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    `maven-publish`
    signing
}

group = providers.gradleProperty("project.group").get()
version = providers.gradleProperty("project.version").get()

val libraryName = providers.gradleProperty("project.name").get()
val libraryNamespace = providers.gradleProperty("project.namespace").get()
val frameworkName = providers.gradleProperty("project.frameworkName").getOrElse("AsyncTrait")

fun boolProperty(name: String, default: Boolean = false): Boolean =
    providers
        .gradleProperty(name)
        .map { it.trim().lowercase() == "true" }
        .getOrElse(default)

val nativeFfiEnabled = boolProperty("project.ffi.native")
val jvmFfiEnabled = boolProperty("project.ffi.jvm")
val napiFfiEnabled = boolProperty("project.ffi.napi")

val ffiNameRaw = providers.gradleProperty("project.ffi.name").getOrElse(frameworkName).trim()
val ffiName = ffiNameRaw.replaceFirstChar { it.uppercase() }
val ffiNameLower = ffiNameRaw.lowercase()
val ffiDefFile = "src/nativeInterop/cinterop/$ffiName.def"
val ffiWrapperBase = "${ffiNameLower}_wrapper"
val ffiNapiDir = "native/node-$ffiNameLower"
val ffiNpmPackage = "@kotlinmania/$ffiNameLower-native-bindings"

val osName = providers.systemProperty("os.name").get().lowercase()
val isWindowsHost = "windows" in osName
val isMacHost = "mac" in osName

val projectKonanDataDirectory =
    providers
        .environmentVariable("KONAN_DATA_DIR")
        .orElse(providers.systemProperty("user.home").map { "$it/.konan" })

abstract class AndroidJniTask : DefaultTask() {
    @get:OutputDirectory
    abstract val nativeDirectory: DirectoryProperty
}

// KGP runs Swift Export in an isolated worker whose classpath is `swiftExportClasspath`.
// Adding a dependency disables KGP's default dependency population, so keep the default
// embeddable runner explicit alongside the IntelliJ coroutines artifact.
val projectDependencyHandler = project.dependencies
configurations.configureEach {
    if (name.contains("androidDeviceTest", ignoreCase = true)) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    if (name == "swiftExportClasspath") {
        dependencies.add(
            projectDependencyHandler.create(
                libs.swift.export.embeddable
                    .get(),
            ),
        )
        dependencies.add(
            projectDependencyHandler.create(
                libs.intellij.coroutines.core.jvm
                    .get(),
            ),
        )
    }
}

// Generated bridge/runtime Kotlin is held to the same diagnostics as project code.
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.allWarningsAsErrors.set(true)
    doFirst {
        check(compilerOptions.allWarningsAsErrors.get()) { "Warnings-as-errors disabled for $path" }
    }
}

kotlin {
    jvmToolchain(
        libs.versions.jvm
            .get()
            .toInt(),
    )

    applyDefaultHierarchyTemplate()

    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_4
        apiVersion = KotlinVersion.KOTLIN_2_4
        allWarningsAsErrors = true
        optIn.addAll(
            "kotlin.time.ExperimentalTime",
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlin.ExperimentalUnsignedTypes",
        )
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xsuppress-version-warnings")
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.get())
        }
    }
    android {
        namespace = libraryNamespace
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        withHostTestBuilder {
            sourceSetTreeName = "test"
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            managedDevices.localDevices.create("pixelApi36") {
                device = "Pixel 6"
                sdkVersion =
                    libs.versions.android.compileSdk
                        .get()
                        .toInt()
                systemImageSource = "aosp"
                require64Bit = true
            }
        }
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.get())
        }
        lint {
            warningsAsErrors = true
        }
    }
    js {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        nodejs {
            testTask {
                useMocha { timeout = "120s" }
            }
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }
    linuxX64()
    linuxArm64()
    mingwX64()
    androidNativeArm64()
    androidNativeX64()

    val xcframework = XCFramework(frameworkName)
    listOf(
        macosArm64(),
        iosArm64(),
        iosSimulatorArm64(),
        tvosArm64(),
        tvosSimulatorArm64(),
        watchosDeviceArm64(),
        watchosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = frameworkName
            isStatic = true
            binaryOption("bundleId", libraryNamespace)
            xcframework.add(this)
        }
    }

    swiftExport {
        moduleName = frameworkName
        flattenPackage = libraryNamespace
        @OptIn(ExperimentalSwiftExportDsl::class)
        configure {
            settings.put("enableCoroutinesSupport", "true")
        }
    }

    // Native FFI — Kotlin/Native cinterop + per-target static wrapper library.
    // Configured per https://kotlinlang.org/docs/multiplatform/multiplatform-ios-dependencies.html#with-cinterop
    if (nativeFfiEnabled) {
        fun ensureKonanDependency(name: String, konanDeps: File, execOps: ExecOperations) {
            val targetDir = File(konanDeps, name)
            if (targetDir.exists()) return
            konanDeps.mkdirs()
            val cacheDir = File(konanDeps, "cache").apply { mkdirs() }
            val archive = File(cacheDir, "$name.tar.gz")
            val url = "https://download.jetbrains.com/kotlin/native/$name.tar.gz"
            if (!archive.exists() || archive.length() == 0L) {
                execOps
                    .exec {
                        commandLine("curl", "-sSL", url, "-o", archive.absolutePath)
                    }.assertNormalExitValue()
            }
            execOps
                .exec {
                    commandLine("tar", "-xzf", archive.absolutePath, "-C", konanDeps.absolutePath)
                }.assertNormalExitValue()
        }

        targets.withType<KotlinNativeTarget>().configureEach {
            val konanTargetName = konanTarget.name
            val targetCapitalized = name.replaceFirstChar { it.uppercase() }
            val wrapperTaskName = "compile${ffiName}WrapperFor$targetCapitalized"
            val wrapperTask =
                tasks.register(wrapperTaskName) {
                    group = "build"
                    description = "Compiles $ffiWrapperBase.c into a static library for $konanTargetName"
                    val cSource = file("src/nativeInterop/cinterop/$ffiWrapperBase.c")
                    val hSource = file("src/nativeInterop/cinterop/$ffiWrapperBase.h")
                    val outDir =
                        layout.buildDirectory
                            .dir("cinterop-targets/$konanTargetName")
                            .get()
                            .asFile
                    val outFile = File(outDir, "$ffiWrapperBase.a")
                    inputs.file(cSource)
                    inputs.file(hSource)
                    outputs.file(outFile)

                    doLast {
                        outDir.mkdirs()
                        if (outFile.exists()) outFile.delete()
                        val tempObj = File(outDir, "$ffiWrapperBase.o")
                        val konanDeps = File(projectKonanDataDirectory.get(), "dependencies")
                        val execOps = project.serviceOf<ExecOperations>()

                        when {
                            isMacHost && konanTarget.family.isAppleFamily -> {
                                val (appleTriple, sdkName) =
                                    when (konanTargetName) {
                                        "macos_arm64" -> "arm64-apple-macos14.0" to "macosx"
                                        "ios_arm64" -> "arm64-apple-ios14.0" to "iphoneos"
                                        "ios_simulator_arm64" -> "arm64-apple-ios14.0-simulator" to "iphonesimulator"
                                        "watchos_device_arm64" -> "arm64-apple-watchos7.0" to "watchos"
                                        "watchos_simulator_arm64" -> "arm64-apple-watchos7.0-simulator" to "watchsimulator"
                                        "tvos_arm64" -> "arm64-apple-tvos14.0" to "appletvos"
                                        "tvos_simulator_arm64" -> "arm64-apple-tvos14.0-simulator" to "appletvsimulator"
                                        else -> "arm64-apple-macos14.0" to "macosx"
                                    }
                                execOps
                                    .exec {
                                        commandLine(
                                            "xcrun",
                                            "-sdk",
                                            sdkName,
                                            "clang",
                                            "-target",
                                            appleTriple,
                                            "-Wall",
                                            "-Wextra",
                                            "-Werror",
                                            "-c",
                                            "-I${cSource.parent}",
                                            cSource.absolutePath,
                                            "-o",
                                            tempObj.absolutePath,
                                        )
                                    }.assertNormalExitValue()
                                execOps
                                    .exec {
                                        commandLine("xcrun", "-sdk", sdkName, "ar", "rcs", outFile.absolutePath, tempObj.absolutePath)
                                    }.assertNormalExitValue()
                            }

                            konanTargetName.startsWith("android") -> {
                                val hostToolchain = if (isMacHost) "osx" else "linux"
                                ensureKonanDependency("target-toolchain-2-$hostToolchain-android_ndk", konanDeps, execOps)
                                ensureKonanDependency("target-sysroot-1-android_ndk", konanDeps, execOps)
                                val ndkDir = File(konanDeps, "target-toolchain-2-$hostToolchain-android_ndk")
                                val sysroot = File(ndkDir, "sysroot")
                                val archPrefix = if (konanTargetName.contains("arm64")) "aarch64-linux-android" else "x86_64-linux-android"
                                val clangBin = File(ndkDir, "bin/${archPrefix}21-clang")
                                val arBin = File(ndkDir, "bin/$archPrefix-ar")
                                execOps
                                    .exec {
                                        commandLine(
                                            clangBin.absolutePath,
                                            "--sysroot=${sysroot.absolutePath}",
                                            "-Wall",
                                            "-Wextra",
                                            "-Werror",
                                            "-c",
                                            "-I${cSource.parent}",
                                            cSource.absolutePath,
                                            "-o",
                                            tempObj.absolutePath,
                                        )
                                    }.assertNormalExitValue()
                                execOps
                                    .exec {
                                        commandLine(arBin.absolutePath, "rcs", outFile.absolutePath, tempObj.absolutePath)
                                    }.assertNormalExitValue()
                            }

                            konanTargetName.startsWith("linux") -> {
                                val (targetTriple, gccDirName) =
                                    if (konanTargetName.contains("arm64")) {
                                        "aarch64-unknown-linux-gnu" to "aarch64-unknown-linux-gnu-gcc-8.3.0-glibc-2.25-kernel-4.9-2"
                                    } else {
                                        "x86_64-unknown-linux-gnu" to "x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2"
                                    }
                                if (!isMacHost && !isWindowsHost) {
                                    execOps
                                        .exec {
                                            commandLine(
                                                "clang",
                                                "-Wall",
                                                "-Wextra",
                                                "-Werror",
                                                "-c",
                                                "-I${cSource.parent}",
                                                cSource.absolutePath,
                                                "-o",
                                                tempObj.absolutePath,
                                            )
                                        }.assertNormalExitValue()
                                    execOps
                                        .exec {
                                            commandLine("ar", "rcs", outFile.absolutePath, tempObj.absolutePath)
                                        }.assertNormalExitValue()
                                } else {
                                    ensureKonanDependency(gccDirName, konanDeps, execOps)
                                    ensureKonanDependency("target-toolchain-2-osx-android_ndk", konanDeps, execOps)
                                    val sysroot = File(konanDeps, "$gccDirName/$targetTriple/sysroot")
                                    val llvmAr = File(konanDeps, "target-toolchain-2-osx-android_ndk/bin/llvm-ar")
                                    execOps
                                        .exec {
                                            commandLine(
                                                "clang",
                                                "--target=$targetTriple",
                                                "--sysroot=${sysroot.absolutePath}",
                                                "-Wall",
                                                "-Wextra",
                                                "-Werror",
                                                "-c",
                                                "-I${cSource.parent}",
                                                cSource.absolutePath,
                                                "-o",
                                                tempObj.absolutePath,
                                            )
                                        }.assertNormalExitValue()
                                    execOps
                                        .exec {
                                            commandLine(llvmAr.absolutePath, "rcs", outFile.absolutePath, tempObj.absolutePath)
                                        }.assertNormalExitValue()
                                }
                            }

                            konanTargetName.startsWith("mingw") -> {
                                ensureKonanDependency("msys2-mingw-w64-x86_64-2", konanDeps, execOps)
                                val mingwSysroot = File(konanDeps, "msys2-mingw-w64-x86_64-2")
                                val arBin =
                                    if (isWindowsHost) {
                                        File(mingwSysroot, "bin/ar.exe").takeIf { it.exists() }?.absolutePath ?: "ar"
                                    } else {
                                        ensureKonanDependency("target-toolchain-2-osx-android_ndk", konanDeps, execOps)
                                        File(konanDeps, "target-toolchain-2-osx-android_ndk/bin/llvm-ar").absolutePath
                                    }
                                execOps
                                    .exec {
                                        commandLine(
                                            "clang",
                                            "--target=x86_64-w64-mingw32",
                                            "--sysroot=${mingwSysroot.absolutePath}",
                                            "-Wall",
                                            "-Wextra",
                                            "-Werror",
                                            "-c",
                                            "-I${cSource.parent}",
                                            cSource.absolutePath,
                                            "-o",
                                            tempObj.absolutePath,
                                        )
                                    }.assertNormalExitValue()
                                execOps
                                    .exec {
                                        commandLine(arBin, "rcs", outFile.absolutePath, tempObj.absolutePath)
                                    }.assertNormalExitValue()
                            }
                        }
                        tempObj.delete()
                        if (!outFile.exists() || outFile.length() == 0L) {
                            throw GradleException("compile${ffiName}Wrapper failed for $konanTargetName at ${outFile.absolutePath}")
                        }
                    }
                }

            compilations.named("main") {
                cinterops.create(ffiNameLower) {
                    definitionFile.set(file(ffiDefFile))
                    packageName("$ffiNameLower.cinterop")
                    includeDirs(file("src/nativeInterop/cinterop"))
                    extraOpts(
                        "-libraryPath",
                        layout.buildDirectory
                            .dir("cinterop-targets/$konanTargetName")
                            .get()
                            .asFile.absolutePath,
                    )
                }
            }
            tasks.matching { it.name == "cinterop${ffiName}$targetCapitalized" }.configureEach {
                dependsOn(wrapperTask)
                inputs.files(wrapperTask)
            }
        }
    }

    // Source set hierarchy and per-source-set dependency declarations aligned with official Kotlin docs:
    //   https://kotlinlang.org/docs/multiplatform/multiplatform-hierarchy.html#manual-configuration
    //   https://kotlinlang.org/docs/multiplatform/multiplatform-add-dependencies.html#0
    //   https://kotlinlang.org/docs/multiplatform/multiplatform-android-dependencies.html
    //   https://kotlinlang.org/docs/multiplatform/multiplatform-ios-dependencies.html
    //   https://kotlinlang.org/docs/multiplatform/multiplatform-advanced-project-structure.html#dependson-and-source-set-hierarchies
    sourceSets {
        commonMain.dependencies {
            api(libs.syn.kotlin)
            api(libs.proc.macro2.kotlin)
            api(libs.quote.kotlin)
            implementation(libs.proc.macro.kotlin)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.collections.immutable)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.futures.kotlin)
            implementation(libs.tracing.kotlin)
        }

        // Custom intermediate source sets for host-OS targets (excluding wasmWasi):
        val hostSystemMain by creating {
            dependsOn(commonMain.get())
            dependencies {
            }
        }
        val hostSystemTest by creating {
            dependsOn(commonTest.get())
            dependencies {
            }
        }

        jvmMain {
            dependsOn(hostSystemMain)
            dependencies {
            }
        }
        jvmTest {
            dependsOn(hostSystemTest)
            dependencies {
            }
        }

        androidMain {
            dependsOn(hostSystemMain)
            dependencies {
            }
        }
        val androidHostTest by getting {
            dependsOn(hostSystemTest)
            dependencies {
            }
        }
        val androidDeviceTest by getting {
            dependsOn(hostSystemTest)
            dependencies {
                implementation(libs.android.test.runner)
            }
        }

        webMain {
            dependsOn(hostSystemMain)
            dependencies {
            }
        }
        webTest {
            dependsOn(hostSystemTest)
            dependencies {
            }
        }
        jsMain.dependencies {
        }
        jsTest.dependencies {
        }
        wasmJsMain.dependencies {
        }
        wasmJsTest.dependencies {
        }
        wasmWasiMain.dependencies {
        }
        wasmWasiTest.dependencies {
        }

        nativeMain {
            dependsOn(hostSystemMain)
            dependencies {
            }
        }
        nativeTest {
            dependsOn(hostSystemTest)
            dependencies {
            }
        }

        // Custom intermediate source sets for POSIX targets under native (apple, linux, androidNative; excluding mingw):
        val posixMain by creating {
            dependsOn(nativeMain.get())
            dependencies {
            }
        }
        val posixTest by creating {
            dependsOn(nativeTest.get())
            dependencies {
            }
        }

        appleMain {
            dependsOn(posixMain)
            dependencies {
            }
        }
        appleTest {
            dependsOn(posixTest)
            dependencies {
            }
        }
        iosMain.dependencies {
        }
        iosTest.dependencies {
        }
        iosArm64Main.dependencies {
        }
        iosArm64Test.dependencies {
        }
        iosSimulatorArm64Main.dependencies {
        }
        iosSimulatorArm64Test.dependencies {
        }
        macosMain.dependencies {
        }
        macosTest.dependencies {
        }
        macosArm64Main.dependencies {
        }
        macosArm64Test.dependencies {
        }
        tvosMain.dependencies {
        }
        tvosTest.dependencies {
        }
        tvosArm64Main.dependencies {
        }
        tvosArm64Test.dependencies {
        }
        tvosSimulatorArm64Main.dependencies {
        }
        tvosSimulatorArm64Test.dependencies {
        }
        watchosMain.dependencies {
        }
        watchosTest.dependencies {
        }
        watchosDeviceArm64Main.dependencies {
        }
        watchosDeviceArm64Test.dependencies {
        }
        watchosSimulatorArm64Main.dependencies {
        }
        watchosSimulatorArm64Test.dependencies {
        }

        linuxMain {
            dependsOn(posixMain)
            dependencies {
            }
        }
        linuxTest {
            dependsOn(posixTest)
            dependencies {
            }
        }
        linuxX64Main.dependencies {
        }
        linuxX64Test.dependencies {
        }
        linuxArm64Main.dependencies {
        }
        linuxArm64Test.dependencies {
        }

        androidNativeMain {
            dependsOn(posixMain)
            dependencies {
            }
        }
        androidNativeTest {
            dependsOn(posixTest)
            dependencies {
            }
        }
        androidNativeArm64Main.dependencies {
        }
        androidNativeArm64Test.dependencies {
        }
        androidNativeX64Main.dependencies {
        }
        androidNativeX64Test.dependencies {
        }

        mingwMain.dependencies {
        }
        mingwTest.dependencies {
        }
        mingwX64Main.dependencies {
        }
        mingwX64Test.dependencies {
        }
    }
}

// Expose all KMP source sets to IntelliJ IDEA's GradleDependencyGraphModelBuilder (`library -> Dependencies`
// in the Gradle Tool Window) without altering `javaExt.sourceSets` for Gradle tasks or `GradleSourceSetModelBuilder`.
afterEvaluate {
    val javaExt = extensions.findByType(JavaPluginExtension::class.java) ?: return@afterEvaluate
    val delegateSourceSets = javaExt.sourceSets
    val syntheticSourceSets = mutableListOf<Any>()
    val attributeMethod =
        AttributeContainer::class.java.getMethod(
            "attribute",
            Attribute::class.java,
            Any::class.java,
        )

    fun copyAttributes(from: AttributeContainer, to: AttributeContainer) {
        from.keySet().forEach { key ->
            val value = from.getAttribute(key) ?: return@forEach
            attributeMethod.invoke(to, key, value)
        }
    }

    for (kss in kotlin.sourceSets) {
        val name = kss.name
        val existing = delegateSourceSets.findByName(name)
        if (existing != null) {
            syntheticSourceSets.add(existing)
            continue
        }
        val targetPrefix =
            when {
                name.endsWith("Main") -> name.removeSuffix("Main")
                name.endsWith("Test") -> name
                else -> name
            }
        val compileBacking =
            configurations.findByName("${name}CompileClasspath")
                ?: configurations.findByName("${targetPrefix}CompileClasspath")
                ?: configurations.findByName("${targetPrefix}CompileKlibraries")
                ?: configurations.findByName("${name}ResolvableDependenciesMetadata")
        val runtimeBacking =
            configurations.findByName("${name}RuntimeClasspath")
                ?: configurations.findByName("${targetPrefix}RuntimeClasspath")
                ?: compileBacking

        if (compileBacking != null) {
            val compileCfgName = "${name}CompileClasspath"
            val runtimeCfgName = "${name}RuntimeClasspath"
            val compileCfg = configurations.findByName(compileCfgName) ?: configurations.create(compileCfgName)
            compileCfg.isCanBeResolved = true
            compileCfg.isCanBeConsumed = false
            if (compileCfg != compileBacking) {
                compileCfg.extendsFrom(compileBacking)
                copyAttributes(compileBacking.attributes, compileCfg.attributes)
            }
            val runtimeCfg = configurations.findByName(runtimeCfgName) ?: configurations.create(runtimeCfgName)
            runtimeCfg.isCanBeResolved = true
            runtimeCfg.isCanBeConsumed = false
            if (runtimeBacking != null && runtimeCfg != runtimeBacking) {
                runtimeCfg.extendsFrom(runtimeBacking)
                copyAttributes(runtimeBacking.attributes, runtimeCfg.attributes)
            }
            val proxySourceSet =
                Proxy.newProxyInstance(
                    SourceSet::class.java.classLoader,
                    arrayOf(SourceSet::class.java),
                ) { _, method, _ ->
                    when (method.name) {
                        "getName" -> name
                        "getCompileClasspathConfigurationName" -> compileCfgName
                        "getRuntimeClasspathConfigurationName" -> runtimeCfgName
                        "toString" -> "source set '$name'"
                        "hashCode" -> name.hashCode()
                        "equals" -> false
                        else -> null
                    }
                }
            syntheticSourceSets.add(proxySourceSet)
        }
    }

    val proxyContainer =
        Proxy.newProxyInstance(
            SourceSetContainer::class.java.classLoader,
            arrayOf(SourceSetContainer::class.java),
        ) { _, method, args ->
            val isDependencyGraphBuilder =
                method.name == "iterator" &&
                    Thread.currentThread().stackTrace.any {
                        it.className.contains("GradleDependencyGraphModelBuilder")
                    }
            if (isDependencyGraphBuilder) {
                syntheticSourceSets.iterator()
            } else if (args == null) {
                method.invoke(delegateSourceSets)
            } else {
                method.invoke(delegateSourceSets, *args)
            }
        }

    val sourceSetsField = DefaultJavaPluginExtension::class.java.getDeclaredField("sourceSets")
    sourceSetsField.isAccessible = true
    sourceSetsField.set(javaExt, proxyContainer)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
    config.setFrom(rootProject.files("detekt.yml"))
    parallel = true
}

ktlint {
    version = libs.versions.ktlintEngine.get()
    filter {
        exclude {
            it.file.toPath().startsWith(
                layout.buildDirectory
                    .get()
                    .asFile
                    .toPath(),
            )
        }
    }
}
tasks.named<Detekt>("detekt") {
    setSource(kotlin.sourceSets.map { it.kotlin })
    exclude {
        it.file.toPath().startsWith(
            layout.buildDirectory
                .get()
                .asFile
                .toPath(),
        )
    }
}

rootProject.extensions.configure<NodeJsEnvSpec> {
    version = libs.versions.node.get()
}
rootProject.extensions.configure<WasmNodeJsEnvSpec> {
    version = libs.versions.node.get()
}
rootProject.extensions.configure<YarnRootEnvSpec> {
    version = libs.versions.yarn.get()
}
rootProject.extensions.configure<WasmYarnRootEnvSpec> {
    version = libs.versions.yarn.get()
}
val npmResolutions =
    mapOf(
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
    node.versions.karmaWebpack.version = "file:${rootProject.file("gradle/npm/karma-webpack").invariantSeparatorsPath}"
}

rootProject.tasks.matching { it.name == "kotlinStoreYarnLock" }.configureEach {
    dependsOn(rootProject.tasks.matching { it.name == "kotlinUpgradeYarnLock" })
}
rootProject.tasks.matching { it.name == "kotlinWasmStoreYarnLock" }.configureEach {
    dependsOn(rootProject.tasks.matching { it.name == "kotlinWasmUpgradeYarnLock" })
}

// ============================================================================
// JNI — JVM & Android C bindings. Gated by project.ffi.jvm=true.
// ============================================================================
if (jvmFfiEnabled) {
    val compileFfiJni =
        tasks.register("compile${ffiName}Jni") {
            group = "build"
            description = "Compiles ${ffiNameLower}_jni.c into a shared library for JVM JNI binding"
            val cSource = file("src/jvmMain/c/${ffiNameLower}_jni.c")
            val outDir =
                layout.buildDirectory
                    .dir("natives")
                    .get()
                    .asFile
            inputs.file(cSource)
            inputs.property("jniHostOs", providers.systemProperty("os.name"))
            inputs.property("jniHostArchitecture", providers.systemProperty("os.arch"))
            outputs.dir(outDir)

            doLast {
                outDir.mkdirs()
                val javaHome = System.getProperty("java.home") ?: System.getenv("JAVA_HOME") ?: ""
                val (libName, osInclude) =
                    when {
                        isWindowsHost -> "${ffiNameLower}_jni.dll" to "win32"
                        isMacHost -> "lib${ffiNameLower}_jni.dylib" to "darwin"
                        else -> "lib${ffiNameLower}_jni.so" to "linux"
                    }
                val outFile = File(outDir, libName)
                val javaInclude = File(javaHome, "include")
                val javaOsInclude = File(javaInclude, osInclude)
                if (!javaInclude.exists()) {
                    throw GradleException("JNI headers are required at $javaInclude")
                }
                val cmd =
                    mutableListOf(
                        "clang",
                        "-shared",
                        "-O2",
                        "-Wall",
                        "-Wextra",
                        "-Werror",
                        "-I${javaInclude.absolutePath}",
                        "-I${javaOsInclude.absolutePath}",
                        cSource.absolutePath,
                        "-o",
                        outFile.absolutePath,
                    )
                if (!isWindowsHost) {
                    cmd.add(2, "-fPIC")
                } else {
                    cmd.addAll(listOf("-lws2_32"))
                }
                if (isMacHost) {
                    cmd.addAll(listOf("-mmacosx-version-min=15.0", "-Wl,-install_name,@rpath/$libName"))
                }
                project
                    .serviceOf<ExecOperations>()
                    .exec {
                        commandLine(cmd)
                    }.assertNormalExitValue()
            }
        }

    val namespacePath = libraryNamespace.replace('.', '/')
    val packageFfiJni =
        tasks.register<Sync>("package${ffiName}Jni") {
            dependsOn(compileFfiJni)
            val hostPlatform =
                when {
                    isWindowsHost -> "windows"
                    isMacHost -> "macos"
                    else -> "linux"
                }
            val hostArchitecture =
                when (providers.systemProperty("os.arch").get().lowercase()) {
                    "aarch64", "arm64" -> "aarch64"
                    "amd64", "x86_64" -> "x86_64"
                    else -> throw GradleException("Unsupported JNI host architecture")
                }
            from(layout.buildDirectory.dir("natives")) {
                include("*.so", "*.dll", "*.dylib")
                into("$namespacePath/native/$hostPlatform-$hostArchitecture")
            }
            from(layout.buildDirectory.dir("jni-distribution")) {
                exclude("$hostPlatform-$hostArchitecture/**")
                into("$namespacePath/native")
            }
            into(layout.buildDirectory.dir("generated/jniResources"))
        }
    kotlin.sourceSets.named("jvmMain") { resources.srcDir(packageFfiJni) }
    tasks.withType<Test>().matching { it.name == "jvmTest" || it.name == "testAndroidHostTest" }.configureEach {
        dependsOn(compileFfiJni)
        systemProperty(
            "java.library.path",
            layout.buildDirectory
                .dir("natives")
                .get()
                .asFile.absolutePath,
        )
    }

    val compileAndroidJni =
        tasks.register<AndroidJniTask>("compileAndroidJni") {
            if (nativeFfiEnabled) {
                dependsOn("compile${ffiName}WrapperForAndroidNativeArm64", "compile${ffiName}WrapperForAndroidNativeX64")
            }
            inputs.file("src/jvmMain/c/${ffiNameLower}_jni.c")
            nativeDirectory.set(layout.buildDirectory.dir("android-jni"))
            doLast {
                if (isWindowsHost) throw GradleException("Build Android JNI payloads on macOS or Linux host")
                val host = if (isMacHost) "osx" else "linux"
                val ndk = File(projectKonanDataDirectory.get(), "dependencies/target-toolchain-2-$host-android_ndk")
                val source = file("src/jvmMain/c/${ffiNameLower}_jni.c")
                val api =
                    libs.versions.android.minSdk
                        .get()
                val execOps = project.serviceOf<ExecOperations>()
                for ((abi, triple) in listOf("arm64-v8a" to "aarch64-linux-android", "x86_64" to "x86_64-linux-android")) {
                    val compiler = File(ndk, "bin/$triple$api-clang")
                    if (!compiler.exists()) throw GradleException("Missing Android JNI compiler $compiler")
                    val output = nativeDirectory.get().asFile.resolve("$abi/lib${ffiNameLower}_jni.so")
                    output.parentFile.mkdirs()
                    execOps
                        .exec {
                            commandLine(
                                compiler.absolutePath,
                                "--sysroot=${ndk.resolve("sysroot")}",
                                "-shared",
                                "-fPIC",
                                "-O2",
                                "-Wall",
                                "-Wextra",
                                "-Werror",
                                "-Wl,-z,max-page-size=16384",
                                source.absolutePath,
                                "-o",
                                output.absolutePath,
                            )
                        }.assertNormalExitValue()
                }
            }
        }
    extensions.getByType<KotlinMultiplatformAndroidComponentsExtension>().onVariants { variant ->
        requireNotNull(variant.sources.jniLibs).addGeneratedSourceDirectory(compileAndroidJni) { it.nativeDirectory }
    }
}

// ============================================================================
// N-API — JS/WASM C bindings. Gated by project.ffi.napi=true.
// ============================================================================
if (napiFfiEnabled) {
    val buildNodeFfi =
        tasks.register<Exec>("buildNode$ffiName") {
            group = "build"
            description = "Builds the native node-$ffiNameLower N-API addon via node-gyp"
            workingDir = file(ffiNapiDir)
            if (isWindowsHost) {
                commandLine("cmd", "/c", "npm install --ignore-scripts && npx node-gyp configure && npx node-gyp build")
            } else {
                commandLine("sh", "-c", "npm install --ignore-scripts && npx node-gyp configure && npx node-gyp build")
            }
        }

    val nodeHostPlatform =
        when {
            isMacHost -> "macos"
            isWindowsHost -> "windows"
            else -> "linux"
        }
    val nodeHostArchitecture =
        when (providers.systemProperty("os.arch").get().lowercase()) {
            "aarch64", "arm64" -> "aarch64"
            "amd64", "x86_64" -> "x86_64"
            else -> throw GradleException("Unsupported Node native architecture")
        }
    val stageNodeRuntime =
        tasks.register<Sync>("stageNode${ffiName}Runtime") {
            dependsOn(buildNodeFfi)
            inputs.property("libraryVersion", project.version.toString())
            from(ffiNapiDir) { include("index.js") }
            from(rootProject.files("LICENSE", "LICENSE-MIT", "LICENSE-APACHE"))
            from("$ffiNapiDir/build/Release") {
                include("*.node")
                rename { "${ffiNameLower}_native.node" }
                into("prebuilds/$nodeHostPlatform-$nodeHostArchitecture")
            }
            from(layout.buildDirectory.dir("napi-distribution")) {
                exclude("$nodeHostPlatform-$nodeHostArchitecture/**")
                into("prebuilds")
            }
            into(layout.buildDirectory.dir("node-runtime-package"))
            doLast {
                destinationDir.resolve("package.json").writeText(
                    JsonOutput.toJson(
                        mapOf(
                            "name" to ffiNpmPackage,
                            "version" to project.version.toString(),
                            "main" to "index.js",
                            "engines" to mapOf("node" to ">=24.15.0"),
                            "files" to listOf("index.js", "prebuilds", "LICENSE*"),
                        ),
                    ) + "\n",
                )
            }
        }
    val nodeRuntimeArchive =
        tasks.register<Tar>("packageNode${ffiName}Runtime") {
            dependsOn(stageNodeRuntime)
            compression = Compression.GZIP
            archiveBaseName.set("kotlinmania-$ffiNameLower-native-bindings")
            archiveVersion.set(project.version.toString())
            archiveExtension.set("tgz")
            destinationDirectory.set(layout.buildDirectory.dir("native-packages"))
            from(stageNodeRuntime) { into("package") }
        }
    publishing.publications.withType<MavenPublication>().matching { it.name == "kotlinMultiplatform" }.configureEach {
        artifact(nodeRuntimeArchive) { classifier = "node-native" }
    }
    val runtimeCopies =
        listOf("js", "wasm").map { runtime ->
            tasks.register<Copy>("copyNode${ffiName}${runtime.replaceFirstChar { it.uppercaseChar() }}") {
                dependsOn(buildNodeFfi)
                val npmInstallTask = if (runtime == "js") "kotlinNpmInstall" else "kotlinWasmNpmInstall"
                dependsOn(rootProject.tasks.matching { it.name == npmInstallTask })
                from(ffiNapiDir) {
                    include("index.js", "package.json", "build/Release/*.node", "prebuilds/**")
                }
                into(rootProject.layout.buildDirectory.dir("$runtime/node_modules/$ffiNpmPackage"))
            }
        }
    val copyNodeFfi =
        tasks.register("copyNode$ffiName") {
            dependsOn(runtimeCopies)
        }
    tasks.matching { it.name.startsWith("jsNodeTest") || it.name.startsWith("wasmJsNodeTest") }.configureEach {
        dependsOn(copyNodeFfi)
    }
}

val linuxArm64Binary =
    kotlin.targets
        .getByName<KotlinNativeTarget>("linuxArm64")
        .binaries
        .getTest(NativeBuildType.DEBUG)
tasks.register<Sync>("prepareLinuxArm64Test") {
    group = "verification"
    description = "Stages the cross-linked Linux ARM64 tests for execution on an ARM64 runner."
    dependsOn(linuxArm64Binary.linkTaskProvider)
    from(linuxArm64Binary.outputFile) { rename { "test.kexe" } }
    into(layout.buildDirectory.dir("linux-arm64-test"))
}

val adb = extensions.getByType<KotlinMultiplatformAndroidComponentsExtension>().sdkComponents.adb
mapOf(
    "androidNativeArm64" to ("arm64-v8a" to "ANDROID_SERIAL_ARM64"),
    "androidNativeX64" to ("x86_64" to "ANDROID_SERIAL_X64"),
).forEach { (targetName, device) ->
    val (abi, serialVariable) = device
    val serial = providers.environmentVariable(serialVariable)
    val binary =
        kotlin.targets
            .getByName<KotlinNativeTarget>(targetName)
            .binaries
            .getTest(NativeBuildType.DEBUG)
    val testExecutable = binary.outputFile
    val remoteExecutable = "/data/local/tmp/async-trait-$abi-test.kexe"
    val installTest =
        tasks.register<Exec>("${targetName}InstallTest") {
            dependsOn(binary.linkTaskProvider)
            onlyIf("$serialVariable must identify a connected $abi Android device") { serial.isPresent }
            doFirst {
                commandLine(adb.get().asFile, "-s", serial.get(), "push", testExecutable, remoteExecutable)
            }
        }
    val runTest =
        tasks.register<Exec>("${targetName}Test") {
            group = "verification"
            description = "Executes the Kotlin/Native tests on the $abi device selected by $serialVariable."
            dependsOn(installTest)
            onlyIf("$serialVariable must identify a connected $abi Android device") { serial.isPresent }
            doFirst {
                commandLine(
                    adb.get().asFile,
                    "-s",
                    serial.get(),
                    "shell",
                    "device_abi=\$(getprop ro.product.cpu.abi); " +
                        "[ \"\$device_abi\" = '$abi' ] || { echo \"Expected $abi, found \$device_abi\" >&2; exit 1; }; " +
                        "chmod 700 '$remoteExecutable' && '$remoteExecutable'",
                )
            }
        }
    tasks.named("allTests") { dependsOn(runTest) }
}

// Compile every declared compilation and link every declared binary, including tests.
// KGP decides which native outputs the current host can produce.
val assemble = tasks.named("assemble")
@OptIn(ExperimentalWasmDsl::class)
kotlin.targets.configureEach {
    val artifactTask = artifactsTaskName
    assemble.configure { dependsOn(artifactTask) }
    compilations.configureEach {
        val compileTask = compileTaskProvider
        assemble.configure { dependsOn(compileTask) }
        if (platformType != KotlinPlatformType.androidJvm) {
            val compilationOutputs = compileAllTaskName
            assemble.configure { dependsOn(compilationOutputs) }
        }
        if (this is KotlinJsCompilation) {
            binaries.configureEach {
                val link = linkTask
                assemble.configure { dependsOn(link) }
                if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
                    val sync = linkSyncTask
                    assemble.configure { dependsOn(sync) }
                }
                if (this is WasmBinary && mode == KotlinJsBinaryMode.PRODUCTION) {
                    val optimize = optimizeTask
                    assemble.configure { dependsOn(optimize) }
                }
            }
        }
        if (platformType == KotlinPlatformType.js || platformType == KotlinPlatformType.wasm) {
            val lockTask = if (platformType == KotlinPlatformType.js) "kotlinUpgradeYarnLock" else "kotlinWasmUpgradeYarnLock"
            compileTask.configure { dependsOn(rootProject.tasks.matching { it.name == lockTask }) }
        }
    }
}
kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    binaries.configureEach {
        val linkTask = linkTaskProvider
        assemble.configure { dependsOn(linkTask) }
    }
}
assemble.configure {
    dependsOn("assembleAndroidDeviceTest")
    if (HostManager.hostIsMac) dependsOn("assemble${frameworkName}XCFramework")
    if (jvmFfiEnabled) dependsOn("compile${ffiName}Jni")
    if (napiFfiEnabled) dependsOn("buildNode$ffiName")
}

// ============================================================================
// Strict Swift 6 Export & Verification
// ============================================================================
val swiftTest =
    tasks.register("swiftTest") {
        group = "verification"
        description = "Builds the Swift Export SPM package with strict Swift 6 and runs swift test against it."
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
            val gradlew = rootProject.file(if (isWindowsHost) "gradlew.bat" else "gradlew").absolutePath

            execOperations
                .exec {
                    workingDir = rootProject.projectDir
                    commandLine(
                        gradlew,
                        ":library:embedSwiftExportForXcode",
                        "--max-workers=2",
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

            val harnessDir = layout.projectDirectory.dir("swift-test-harness").asFile
            execOperations
                .exec {
                    workingDir = harnessDir
                    commandLine("swift", "package", "reset")
                }.assertNormalExitValue()

            val developerDir =
                ByteArrayOutputStream().use { out ->
                    execOperations
                        .exec {
                            commandLine("xcode-select", "-p")
                            standardOutput = out
                        }.assertNormalExitValue()
                    out.toString().trim()
                }
            val swiftTestCommand =
                mutableListOf(
                    "swift",
                    "test",
                    "--verbose",
                    "--parallel",
                    "-Xswiftc",
                    "-swift-version",
                    "-Xswiftc",
                    "6",
                    "-Xswiftc",
                    "-warnings-as-errors",
                )
            if (developerDir.endsWith("CommandLineTools")) {
                val frameworks = "$developerDir/Library/Developer/Frameworks"
                swiftTestCommand +=
                    listOf(
                        "-Xswiftc",
                        "-F",
                        "-Xswiftc",
                        frameworks,
                        "-Xlinker",
                        "-F",
                        "-Xlinker",
                        frameworks,
                        "-Xlinker",
                        "-rpath",
                        "-Xlinker",
                        frameworks,
                        "-Xlinker",
                        "-rpath",
                        "-Xlinker",
                        "$developerDir/Library/Developer/usr/lib",
                    )
            }
            val swiftTestOutput = ByteArrayOutputStream()
            val swiftTestResult =
                execOperations.exec {
                    workingDir = harnessDir
                    commandLine(swiftTestCommand)
                    standardOutput = swiftTestOutput
                    errorOutput = swiftTestOutput
                    isIgnoreExitValue = true
                }
            val swiftTestLog = swiftTestOutput.toString()
            println(swiftTestLog)
            swiftTestResult.assertNormalExitValue()
            if (!swiftTestLog.contains(Regex("""Test run with [1-9]\d* tests?"""))) {
                throw GradleException("swift test ran no Swift Testing tests (toolchain: $developerDir)")
            }
        }
    }

tasks.register("swiftExportSmokeTest") {
    group = "verification"
    description = "Alias for swiftTest."
    dependsOn(swiftTest)
}

val runManagedAndroidDeviceTests = boolProperty("android.managedDeviceTests")
tasks.register("test") {
    group = "verification"
    description = "Runs all KMP test environments, Android host tests, and Swift on macOS."
    dependsOn("allTests", "testAndroidHostTest")
    if (runManagedAndroidDeviceTests) dependsOn("pixelApi36AndroidDeviceTest")
    if (HostManager.hostIsMac) dependsOn(swiftTest)
}
tasks.named("check") {
    dependsOn("test", "ktlintCheck", "detekt")
}
tasks.named("build") {
    description = "Compiles and packages every supported output; run test separately to execute tests."
    setDependsOn(listOf(assemble, "ktlintCheck", "detekt"))
}
if (HostManager.hostIsMac) {
    val verifyAppleArtifacts =
        tasks.register<Exec>("verifyAppleArtifacts") {
            group = "verification"
            description = "Checks compiled Apple framework architectures, platforms, headers and modules."
            dependsOn("assemble${frameworkName}XCFramework")
            val verifier = rootProject.file("verification/verify_apple_artifacts.py")
            val frameworks = layout.buildDirectory.dir("XCFrameworks")
            val receipt = layout.buildDirectory.file("reports/publication/apple-frameworks.json")
            inputs.file(verifier)
            inputs.dir(frameworks)
            outputs.file(receipt)
            commandLine(
                "python3",
                verifier,
                frameworks.get().asFile,
                "--module",
                frameworkName,
                "--receipt",
                receipt.get().asFile,
            )
        }
    tasks.named("build") { dependsOn(verifyAppleArtifacts) }
}

val javadocJar =
    tasks.register<Jar>("javadocJar") {
        archiveClassifier = "javadoc"
        from(rootProject.file("README.md"))
    }

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "kotlinMultiplatform") libraryName else "$libraryName-${name.lowercase()}"
        artifact(javadocJar)
        pom {
            name = libraryName
            description = providers.gradleProperty("project.pom.description")
            inceptionYear = "2026"
            url = "https://github.com/KotlinMania/$libraryName"
            licenses {
                license {
                    name = providers.gradleProperty("project.pom.licenseName")
                    url = providers.gradleProperty("project.pom.licenseUrl")
                    distribution = "repo"
                }
            }
            developers {
                developer {
                    id = "sydneyrenee"
                    name = "Sydney Renee"
                    url = "https://github.com/sydneyrenee"
                }
            }
            scm {
                url = "https://github.com/KotlinMania/$libraryName"
                connection = "scm:git:https://github.com/KotlinMania/$libraryName.git"
                developerConnection = "scm:git:ssh://git@github.com/KotlinMania/$libraryName.git"
            }
        }
    }
    repositories {
        maven {
            name = "centralPortalStaging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

val signingKey = providers.gradleProperty("signingInMemoryKey")
signing {
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(
            providers.gradleProperty("signingInMemoryKeyId").orNull,
            signingKey.get(),
            providers.gradleProperty("signingInMemoryKeyPassword").orNull,
        )
        sign(publishing.publications)
    }
}

val stagedPublications =
    tasks.withType<PublishToMavenRepository>().matching {
        it.repository.name == "centralPortalStaging"
    }
stagedPublications.configureEach {
    dependsOn(tasks.withType<Sign>())
}
val centralPortalBundle =
    tasks.register<Zip>("centralPortalBundle") {
        group = "publishing"
        dependsOn(stagedPublications)
        from(layout.buildDirectory.dir("staging-deploy"))
        exclude("**/maven-metadata.*")
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveFileName = "$libraryName-$version-bundle.zip"
        destinationDirectory = layout.buildDirectory.dir("central-portal")
    }
publishing.publications.withType<MavenPublication>().configureEach {
    val publicationDirectory = "${groupId.replace('.', '/')}/$artifactId/$version"
    centralPortalBundle.configure { include("$publicationDirectory/**") }
}

val testAndroidPublicationVerifier =
    tasks.register<Exec>("testAndroidPublicationVerifier") {
        group = "verification"
        description = "Exercises Android publication acceptance and invalid-payload rejection against the actual bundle."
        dependsOn(centralPortalBundle)
        val testScript = rootProject.layout.projectDirectory.file("verification/test_android_publication.py")
        val verifier = rootProject.layout.projectDirectory.file("verification/verify_android_publication.py")
        inputs.files(testScript, verifier, centralPortalBundle.flatMap { it.archiveFile })
        commandLine(
            "python3",
            testScript.asFile,
            centralPortalBundle
                .get()
                .archiveFile
                .get()
                .asFile,
            "--group",
            project.group.toString(),
            "--module",
            libraryName,
            "--version",
            project.version.toString(),
            "--scratch",
            layout.buildDirectory
                .dir("publication-verifier-tests")
                .get()
                .asFile,
        )
    }

val verifyAndroidPublication =
    tasks.register<Exec>("verifyAndroidPublication") {
        group = "verification"
        description = "Verifies the Android AAR, POM and metadata in the actual Central bundle."
        dependsOn(centralPortalBundle, testAndroidPublicationVerifier)
        val verifier = rootProject.layout.projectDirectory.file("verification/verify_android_publication.py")
        val bundle = centralPortalBundle.flatMap { it.archiveFile }
        val receipt = layout.buildDirectory.file("reports/publication/android.json")
        inputs.file(verifier)
        inputs.file(bundle)
        outputs.file(receipt)
        commandLine(
            "python3",
            verifier.asFile,
            bundle.get().asFile,
            "--group",
            project.group.toString(),
            "--module",
            libraryName,
            "--version",
            project.version.toString(),
            "--receipt",
            receipt.get().asFile,
        )
    }

tasks.register<Sync>("prepareAndroidPublicationVerification") {
    group = "verification"
    description = "Extracts verified root and Android publications for the independent consumer."
    dependsOn(verifyAndroidPublication)
    from(centralPortalBundle.flatMap { it.archiveFile }.map { zipTree(it.asFile) }) {
        val coordinatePath = project.group.toString().replace('.', '/')
        include("$coordinatePath/$libraryName/$version/**")
        include("$coordinatePath/$libraryName-android/$version/**")
    }
    into(layout.buildDirectory.dir("verification-android-repository"))
}

val deploymentBundle = centralPortalBundle.flatMap { it.archiveFile }
val verifyCentralPortalBundle =
    tasks.register<Exec>("verifyCentralPortalBundle") {
        group = "verification"
        description = "Checks every KMP publication and its payload in the actual Central bundle."
        dependsOn(centralPortalBundle)
        val verifier = rootProject.file("verification/verify_bundle.py")
        val receipt = layout.buildDirectory.file("reports/publication/bundle.json")
        inputs.file(verifier)
        inputs.file(deploymentBundle)
        inputs.property("requireSignatures", signingKey.isPresent)
        outputs.file(receipt)
        commandLine(
            "python3",
            verifier,
            deploymentBundle.get().asFile,
            "--group",
            project.group.toString(),
            "--module",
            libraryName,
            "--version",
            project.version.toString(),
            "--namespace",
            libraryNamespace,
            "--receipt",
            receipt.get().asFile,
        )
        if (signingKey.isPresent) args("--require-signatures")
    }
tasks.register<Sync>("preparePublicationVerification") {
    group = "verification"
    description = "Extracts the verified Central bundle for independent consumer resolution."
    dependsOn(verifyCentralPortalBundle)
    from(zipTree(deploymentBundle))
    into(layout.buildDirectory.dir("verification-repository"))
}

val centralUsername = providers.gradleProperty("mavenCentralUsername")
val centralPassword = providers.gradleProperty("mavenCentralPassword")
val publishingType = providers.gradleProperty("centralPublishingType").orElse("USER_MANAGED")
val deploymentName = "$libraryName-$version"
val hasSigningKey = signingKey.isPresent
tasks.register("publishToCentralPortal") {
    group = "publishing"
    description = "Uploads signed publications and waits for Central Portal validation."
    dependsOn(verifyCentralPortalBundle, verifyAndroidPublication)
    doLast {
        require(hasSigningKey) { "signingInMemoryKey is required for Central publication." }
        require(centralUsername.isPresent && centralPassword.isPresent) { "Central Portal credentials are required." }
        val mode = publishingType.get()
        require(mode in setOf("USER_MANAGED", "AUTOMATIC")) { "Unsupported centralPublishingType: $mode" }
        val token = Base64.getEncoder().encodeToString("${centralUsername.get()}:${centralPassword.get()}".toByteArray())
        val bundle = deploymentBundle.get().asFile
        val boundary = "CentralPortal${UUID.randomUUID()}"
        val body =
            "--$boundary\r\nContent-Disposition: form-data; name=\"bundle\"; filename=\"${bundle.name}\"\r\nContent-Type: application/zip\r\n\r\n".toByteArray() +
                bundle.readBytes() + "\r\n--$boundary--\r\n".toByteArray()
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()
        val response =
            client.send(
                HttpRequest
                    .newBuilder(URI("https://central.sonatype.com/api/v1/publisher/upload?name=$deploymentName&publishingType=$mode"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "multipart/form-data; boundary=$boundary")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )
        check(response.statusCode() in 200..299) { "Central upload failed: HTTP ${response.statusCode()}" }
        val deploymentId = UUID.fromString(response.body().trim())
        logger.lifecycle("Central deployment: $deploymentId")
        repeat(120) {
            val status =
                client.send(
                    HttpRequest
                        .newBuilder(URI("https://central.sonatype.com/api/v1/publisher/status?id=$deploymentId"))
                        .timeout(Duration.ofSeconds(30))
                        .header("Authorization", "Bearer $token")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )
            check(status.statusCode() in 200..299) { "Central status failed: HTTP ${status.statusCode()}" }
            val state = (JsonSlurper().parseText(status.body()) as Map<*, *>)["deploymentState"]
            check(state != "FAILED") { "Central rejected deployment $deploymentId: ${status.body()}" }
            if (state == "PUBLISHED" || (mode == "USER_MANAGED" && state == "VALIDATED")) {
                logger.lifecycle("Central deployment $deploymentId: $state")
                return@doLast
            }
            Thread.sleep(10_000)
        }
        error("Central deployment $deploymentId has not reached the required state; inspect it before retrying.")
    }
}
