import SwiftUI
import Observation
import CoreNetwork
import CoreData

@Observable
public class ChatViewModel {
    public var inputText: String = ""
    public var trajectories: [TrajectoryStep] {
        return AgentProtocol.shared.currentTrajectory
    }
    
    public init() {}
    
    public func connectToAgent() {
        if let token = TokenStore.shared.pairingToken {
            AgentProtocol.shared.connect(token: token)
        }
    }
    
    public func sendMessage() {
        guard !inputText.isEmpty else { return }
        // Implement message sending over WebSocket
        // AgentProtocol.shared.sendMessage(inputText)
        inputText = ""
    }
    
    public func disconnect() {
        AgentProtocol.shared.disconnect()
        TokenStore.shared.clearToken()
    }
}
