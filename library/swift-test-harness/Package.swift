// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "AsyncTraitConsumerTests",
    platforms: [.macOS(.v15)],
    dependencies: [
        .package(name: "AsyncTrait", path: "../build/SPMPackage/macosArm64/Debug")
    ],
    targets: [
        .testTarget(
            name: "SwiftTestHarnessTests",
            dependencies: [.product(name: "AsyncTraitLibrary", package: "AsyncTrait")],
            linkerSettings: [.unsafeFlags(["-L", "../build/swift-test", "-lAsyncTrait"])]
        ),
        .testTarget(
            name: "AsyncTraitConsumerTests",
            dependencies: [.product(name: "AsyncTraitLibrary", package: "AsyncTrait")],
            linkerSettings: [.unsafeFlags(["-L", "../build/swift-test", "-lAsyncTrait"])]
        )
    ]
)
