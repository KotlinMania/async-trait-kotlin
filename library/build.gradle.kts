import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import dev.detekt.gradle.Detekt
import groovy.json.JsonSlurper
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.plugins.signing.Sign
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.WasmBinary
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import java.util.UUID

plugins {
    alias(libs.plugins.kotlinMultiplatform)
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
val frameworkName = providers.gradleProperty("project.frameworkName").get()

kotlin {
    jvmToolchain(
        libs.versions.jvm
            .get()
            .toInt(),
    )
    compilerOptions {
        allWarningsAsErrors = true
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
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
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
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.syn.kotlin)
            api(libs.proc.macro2.kotlin)
            api(libs.quote.kotlin)
            implementation(libs.proc.macro.kotlin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.android.test.runner)
        }
    }
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
    node.versions.karmaWebpack.version = "file:${rootProject.file("gradle/npm/karma-webpack").invariantSeparatorsPath}"
}

val linuxArm64Binary =
    kotlin.targets
        .getByName<KotlinNativeTarget>("linuxArm64")
        .binaries
        .getTest(NativeBuildType.DEBUG)
val linuxArm64Test =
    tasks.register<KotlinNativeHostTest>("linuxArm64Test") {
        group = "verification"
        description = "Executes the Linux ARM64 test binary on a Linux ARM64 host."
        dependsOn(linuxArm64Binary.linkTaskProvider)
        executable = linuxArm64Binary.outputFile
        targetName = "linuxArm64"
        enabled = HostManager.host == KonanTarget.LINUX_ARM64
    }
tasks.named("allTests") {
    dependsOn(linuxArm64Test)
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
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
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
}

val swiftTest =
    tasks.register<Exec>("swiftTest") {
        group = "verification"
        description = "Tests the Swift Export package produced for the macOS host."
        dependsOn("embedSwiftExportForXcode")
        workingDir = file("swift-test-harness")
        environment(
            "SDK_NAME" to "macosx", "CONFIGURATION" to "Debug", "ARCHS" to "arm64",
            "BUILT_PRODUCTS_DIR" to layout.buildDirectory.dir("swift-test").get().asFile.absolutePath,
            "TARGET_BUILD_DIR" to layout.buildDirectory.dir("swift-test").get().asFile.absolutePath,
            "FRAMEWORKS_FOLDER_PATH" to "Frameworks", "MACOSX_DEPLOYMENT_TARGET" to "15.0",
            "DEPLOYMENT_TARGET_SETTING_NAME" to "MACOSX_DEPLOYMENT_TARGET",
        )
        commandLine("swift", "test", "--parallel", "-Xswiftc", "-warnings-as-errors")
    }

tasks.register("test") {
    group = "verification"
    description = "Runs all KMP test environments, Android device tests, and Swift on macOS."
    dependsOn("allTests", "testAndroidHostTest", "pixelApi36AndroidDeviceTest")
    if (HostManager.hostIsMac) dependsOn(swiftTest)
}
tasks.named("check") {
    dependsOn("test")
}
tasks.named("build") {
    description = "Compiles and packages every supported output; run test separately to execute tests."
    setDependsOn(listOf(assemble, "ktlintCheck", "detekt"))
}
if (HostManager.hostIsMac) {
    val verifyAppleArtifacts = tasks.register<Exec>("verifyAppleArtifacts") {
        group = "verification"
        description = "Checks compiled Apple framework architectures, platforms, headers and modules."
        dependsOn("assemble${frameworkName}XCFramework")
        val verifier = rootProject.file("verification/verify_apple_artifacts.py")
        val frameworks = layout.buildDirectory.dir("XCFrameworks")
        val receipt = layout.buildDirectory.file("reports/publication/apple-frameworks.json")
        inputs.file(verifier)
        inputs.dir(frameworks)
        outputs.file(receipt)
        commandLine("python3", verifier, frameworks.get().asFile,
            "--module", frameworkName, "--receipt", receipt.get().asFile)
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
            centralPortalBundle.get().archiveFile.get().asFile,
            "--group",
            project.group.toString(),
            "--module",
            libraryName,
            "--version",
            project.version.toString(),
            "--scratch",
            layout.buildDirectory.dir("publication-verifier-tests").get().asFile,
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
            "python3", verifier, deploymentBundle.get().asFile,
            "--group", project.group.toString(), "--module", libraryName,
            "--version", project.version.toString(), "--namespace", libraryNamespace,
            "--receipt", receipt.get().asFile,
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
