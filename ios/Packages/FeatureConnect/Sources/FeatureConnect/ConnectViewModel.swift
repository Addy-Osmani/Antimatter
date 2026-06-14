import SwiftUI
import Observation
import CoreData
import CoreNetwork
import CoreUI

@Observable
public class ConnectViewModel {
    public var inputToken: String = ""
    public var isConnecting: Bool = false
    public var errorMessage: String?
    
    public init() {}
    
    public func connect() {
        guard !inputToken.isEmpty else {
            errorMessage = "Please enter a pairing token."
            return
        }
        
        isConnecting = true
        errorMessage = nil
        
        // In a true production environment, we wait for AgentProtocol to successfully ping before saving.
        // For Native iOS architecture, we'll assign the token, attempt connect, and let the coordinator react.
        TokenStore.shared.pairingToken = inputToken
        
        // Timeout check for handshake
        Task {
            try? await Task.sleep(nanoseconds: 3_000_000_000) // 3 seconds
            if !AgentProtocol.shared.isConnected {
                await MainActor.run {
                    self.isConnecting = false
                    self.errorMessage = "Failed to connect to local agent daemon."
                    TokenStore.shared.clearToken()
                }
            }
        }
    }
}
