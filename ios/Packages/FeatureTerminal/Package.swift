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
        .package(path: "../CoreData"),
        .package(path: "../CoreUI"),
        .package(url: "https://github.com/migueldeicaza/SwiftTerm.git", from: "1.0.0")
    ],
    targets: [
        .target(
            name: "FeatureTerminal",
            dependencies: [
                "CoreNetwork", 
                "CoreData", 
                "CoreUI",
                .product(name: "SwiftTerm", package: "SwiftTerm")
            ]
        )
    ]
)
