import Foundation
import Observation

@Observable
public class TokenStore {
    public static let shared = TokenStore()
    
    public var pairingToken: String? {
        didSet {
            if let token = pairingToken, let data = token.data(using: .utf8) {
                try? KeychainManager.shared.save(key: "antimatter_pairing_token", data: data)
            } else if pairingToken == nil {
                KeychainManager.shared.delete(key: "antimatter_pairing_token")
            }
        }
    }
    
    public var hasToken: Bool {
        return pairingToken != nil && !pairingToken!.isEmpty
    }
    
    private init() {
        if let data = KeychainManager.shared.load(key: "antimatter_pairing_token"),
           let token = String(data: data, encoding: .utf8) {
            self.pairingToken = token
        }
    }
    
    public func clearToken() {
        self.pairingToken = nil
    }
}
