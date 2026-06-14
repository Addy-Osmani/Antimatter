import SwiftUI
import FeatureConnect
import FeatureChat
import FeatureTerminal
import FeatureFiles
import CoreData
import CoreUI

@main
struct AntimatterApp: App {
    var body: some Scene {
        WindowGroup {
            AppCoordinator()
        }
    }
}
