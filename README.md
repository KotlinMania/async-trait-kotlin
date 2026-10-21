# async-trait-kotlin

A Kotlin Multiplatform implementation of the token transformation performed by
Rust's [async-trait](https://github.com/dtolnay/async-trait), based on version
0.1.89. It parses trait and implementation items with `syn-kotlin`, transforms
async method signatures and bodies, and emits tokens with `quote-kotlin` and
`proc-macro2-kotlin`.

The implementation and regression tests live in `library/src/commonMain` and
`library/src/commonTest`. This transformation does not require platform I/O, so
all targets share the same implementation. Kotlin's default hierarchy connects
the native, Apple, Linux, Android Native and web source sets.

## Gradle structure

- `settings.gradle.kts` declares repositories and includes `:library`.
- The root `build.gradle.kts` declares plugin aliases and forwards build, check and test to `:library`.
- `gradle/libs.versions.toml` controls plugin, SDK, library and tool versions.
- `gradle.properties` holds Gradle options and publication identity.
- `library/build.gradle.kts` applies the plugins and declares every target in
  one `kotlin {}` block, including Android, source sets and Swift Export.
- `verification/consumer` is an independent build that resolves publications
  extracted from the verified Central bundle. It does not use project dependencies
  or source substitution.

The declared targets are JVM, Android, JavaScript, Wasm/JS, Wasm/WASI, Linux x64
and ARM64, Windows x64, Android Native x64 and ARM64, macOS ARM64, iOS ARM64 and
simulator ARM64, tvOS ARM64 and simulator ARM64, and watchOS device ARM64 and
simulator ARM64. There are no 32-bit or Apple x64 targets.

## Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:async-trait-kotlin:0.1.2")
}
```

## API

The Maven identity is `io.github.kotlinmania:async-trait-kotlin`; the version
for this checkout is in `gradle.properties`. Kotlin consumers can call the
concrete text API:

```kotlin
import io.github.kotlinmania.asynctrait.expandAsyncTrait

val expansion = expandAsyncTrait("", "trait Service { async fn run(&self); }")
check(expansion.isSuccess) { expansion.error.orEmpty() }
println(expansion.source)
```

`asyncTrait` and `asyncTraitOrThrow` accept token streams. Their public token
and syntax-tree types are supplied as API dependencies, so a consumer does not
need to repeat the three implementation-library coordinates.

`asyncTrait` returns Syn's concrete `TokenStreamParseResult`, containing either
`value: TokenStream?` or `error: SynError?`. `parseArgs` and `Args.parse` return
the concrete `ArgsParseResult`, containing either `value: Args?` or the same typed
diagnostic. Both result types preserve the original value or diagnostic object.
`parseArgs` accepts either a complete `TokenStream` or an existing `ParseStream`.

`asyncTraitOrThrow` retains its token-stream signature and throws an
`IllegalArgumentException` on a returned parse failure, using an internal exception
that carries the original diagnostic. Callers that caught `SynError` should inspect
the result from `asyncTrait` or catch `IllegalArgumentException` at their throwing
boundary. The diagnostic itself is not the exception.

Swift callers can inspect concrete token, argument and diagnostic properties,
or use `expandAsyncTrait` for its text `source`, `error` and `isSuccess` properties.
The generated direct-export `asyncTraitOrThrow` declaration does not have Swift
`throws`. Inspect result properties for Swift failure handling.

These concrete return contracts replace `SynResult<TokenStream>` and
`SynResult<Args>` at the public boundary. This changes binary signatures;
compiled consumers must rebuild, and explicitly typed result variables must use
the concrete types. Code passing `::parseArgs` to a generic parser should call
the complete-token `parseArgs(tokens)` entry point instead. Generic parser
callbacks remain an internal implementation detail here; public result classes
do not inherit from a generic result and do not expose generic `fold` or `map`
callbacks.

## Build and test

Use JDK 21 and an Android SDK containing the catalog's compile SDK. Android
Studio's `local.properties` or the standard `ANDROID_HOME` environment variable
selects the SDK.

```shell
./gradlew :library:test :library:build --continue
```

`build` compiles every declared main and test compilation, links every native
binary, creates Android AAR and device-test APK outputs, JVM and web archives,
and debug/release Apple frameworks and XCFrameworks on macOS. It also runs
ktlint and detekt. `test` executes the KMP test environments, Android host and
managed-device tests, and the Swift consumer on macOS. `check` includes `test`.
Building a test executable is not counted as executing its tests.

JavaScript and Wasm/JS browser tests use Chrome Headless. Node runs JavaScript,
Wasm/JS and Wasm/WASI tests. Apple simulator tests require an installed runtime
and device for each of iOS, tvOS and watchOS. Windows tests must run on Windows. Linux ARM64 tests are cross-linked on
Linux x64 with `:library:prepareLinuxArm64Test`, then the exact hashed AArch64
executable runs on the Linux ARM64 CI host. Kotlin/Native does not provide a
Linux ARM64 compiler host. The runtime guard verifies the real host, ELF
architecture, artifact hash and complete nonempty test results; raw output,
JSON and JUnit reports are retained. A skipped runner is not successful
platform validation.

For Android Native, set `ANDROID_SERIAL_ARM64` and/or `ANDROID_SERIAL_X64` to
connected devices reported by `adb devices`. The `androidNativeArm64Test` and
`androidNativeX64Test` tasks verify the device ABI and execute the compiled
native test binary through adb. They are part of `allTests` and `test`; absent
device selections produce explicit skipped tasks. The managed Android
emulator is owned and stopped by the Android Gradle plugin, independently of
these connected-device runners.

Swift Export's Xcode integration requires its standard build environment. For
a macOS command-line validation, provide:

```shell
SDK_NAME=macosx CONFIGURATION=Debug ARCHS=arm64 \
BUILT_PRODUCTS_DIR="$PWD/library/build/swift-test" \
TARGET_BUILD_DIR="$PWD/library/build/swift-test" \
FRAMEWORKS_FOLDER_PATH=Frameworks MACOSX_DEPLOYMENT_TARGET=15.0 \
DEPLOYMENT_TARGET_SETTING_NAME=MACOSX_DEPLOYMENT_TARGET \
./gradlew :library:test :library:build --continue
```

The Swift tests import the generated `AsyncTrait` module and exercise real
expansion and diagnostics, including malformed trait and implementation bodies.
The Swift test command passes `-swift-version 6` and `-warnings-as-errors`
through SwiftPM to the harness and its source dependencies. Verbose compiler
commands record the effective language mode for validation. Kotlin and Swift
warnings are build failures. Full local KMP validation is the
primary gate before consumer CI or release; individual platform checks do not
replace it.

## Android publication acceptance

The independently staged AAR must contain the expansion bytecode, manifest,
matching POM/module metadata, and the public Syn, proc-macro2 and quote API dependencies.
Eight Python regression cases validate the real bundle and reject invalid payloads.
The separate Android consumer forces three named Kotlin behavior tests to execute
and rejects missing, skipped or failing XML cases.

```shell
./gradlew :library:prepareAndroidPublicationVerification --no-configuration-cache
env -u ANDROID_SDK_ROOT ./gradlew -p verification/android-consumer \
  verifyAndroidPublication --refresh-dependencies --no-configuration-cache
```

The Android reusable workflow and `publish=false` dry run retain these checks.
The standard `ANDROID_HOME` or root `local.properties` selects the SDK; private
SDK paths are not copied between checkouts.

## Publications

Gradle's `maven-publish` and `signing` plugins produce the root multiplatform
metadata and target publications. Android selects an AAR; JVM selects its JAR;
native, JavaScript and Wasm consumers select platform-specific KLIBs. The root
JAR contains Kotlin metadata; it does not contain JVM bytecode. Apple KLIB
publications and the separately built XCFrameworks are distinct artifacts.

```shell
./gradlew :library:preparePublicationVerification
./gradlew -p verification/consumer -PlibraryVersion=0.1.3 verifyPublication allTests --refresh-dependencies --continue
```

`centralPortalBundle` stages Maven files under `library/build/staging-deploy`
and creates a ZIP under `library/build/central-portal`. It includes only the
current publication coordinates and version. It does not upload.
`verifyCentralPortalBundle` inspects the ZIP's AAR, compiled Kotlin/JVM JAR,
KLIB payloads, metadata links, hashes and publication variants.
`preparePublicationVerification` extracts that checked ZIP into the consumer's
exclusive repository. The consumer checks selected target variants and compiles
its real API calls against them. Receipts are written under `build/reports/publication`
in each build.

On macOS, `build` also verifies the debug/release XCFramework headers, modules,
ARM64 architectures and compiled Mach-O device/simulator platform identifiers.
The separate Swift Export consumer remains part of `test`.

Supply `signingInMemoryKey`, `signingInMemoryKeyId` and
`signingInMemoryKeyPassword` through secure Gradle properties to sign the files.

`publishToCentralPortal` verifies the signed bundle and requires Central Portal credentials. Its
default is `USER_MANAGED`, which waits for Portal validation. Use
`-PcentralPublishingType=AUTOMATIC` to request publication. The task reports the
deployment ID and fails if validation fails. Inspect that deployment before
retrying an interrupted upload.

The validation workflow is manual or reusable; it has no push or pull-request
trigger. It declares Apple simulator, Linux x64/ARM64, Windows x64, Android x64,
JVM, browser and Node coverage. Android ARM64 execution requires an actual
connected ARM64 device or emulator. Publication waits for the validation
workflow at the same revision.

## License

This port retains the existing MIT license in [LICENSE](LICENSE). The upstream
async-trait crate is available under MIT or Apache-2.0.
