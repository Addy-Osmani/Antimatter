import SwiftUI
import Observation
import CoreUI

@Observable
public class TerminalViewModel {
    public var logs: [String] = []
    
    public init() {
        // Listen for logs broadcasted by AgentProtocol
        NotificationCenter.default.addObserver(forName: Notification.Name("NewTerminalLog"), object: nil, queue: .main) { notification in
            if let log = notification.object as? String {
                self.appendLog(log)
            }
        }
    }
    
    public func appendLog(_ log: String) {
        let cleanLog = stripANSI(from: log)
        Task { @MainActor in
            self.logs.append(cleanLog)
            // Prevent memory leaks by capping log history to 1000 lines
            if self.logs.count > 1000 {
                self.logs.removeFirst(self.logs.count - 1000)
            }
        }
    }
    
    // Utility to strip nasty ANSI color codes from daemon stdout
    private func stripANSI(from string: String) -> String {
        let pattern = "\u{001B}\\[[0-9;]*[a-zA-Z]"
        do {
            let regex = try NSRegularExpression(pattern: pattern, options: .caseInsensitive)
            let range = NSRange(location: 0, length: string.utf16.count)
            return regex.stringByReplacingMatches(in: string, options: [], range: range, withTemplate: "")
        } catch {
            return string
        }
    }
}
