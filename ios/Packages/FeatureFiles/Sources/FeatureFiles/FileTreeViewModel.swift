import SwiftUI
import Observation
import CoreUI

public struct FileNode: Identifiable {
    public let id = UUID()
    public let name: String
    public let isDirectory: Bool
    public var children: [FileNode]?
    
    public init(name: String, isDirectory: Bool, children: [FileNode]? = nil) {
        self.name = name
        self.isDirectory = isDirectory
        self.children = children
    }
}

@Observable
public class FileTreeViewModel {
    public var rootFiles: [FileNode] = []
    
    public init() {
        // Placeholder data simulating the daemon's file tree
        self.rootFiles = [
            FileNode(name: "antimatter-bridge", isDirectory: true, children: [
                FileNode(name: "package.json", isDirectory: false),
                FileNode(name: "src", isDirectory: true, children: [
                    FileNode(name: "index.ts", isDirectory: false)
                ])
            ]),
            FileNode(name: "README.md", isDirectory: false)
        ]
    }
}
