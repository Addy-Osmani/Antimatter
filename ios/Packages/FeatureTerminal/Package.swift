// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "FeatureTerminal",
    platforms: [.iOS(.v17)],
    products: [
        .library(
            name: "FeatureTerminal",
            targets: ["FeatureTerminal"]),
    ],
    dependencies: [
        .package(path: "../CoreNetwork"),
        .package(path: "../CoreUI")
    ],
    targets: [
        .target(
            name: "FeatureTerminal",
            dependencies: ["CoreNetwork", "CoreUI"]),
        .testTarget(
            name: "FeatureTerminalTests",
            dependencies: ["FeatureTerminal"]),
    ]
)
