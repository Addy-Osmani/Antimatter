import Foundation
import Observation

public struct BridgeConfig: Codable {
    public let tunnelUrl: String
    
    enum CodingKeys: String, CodingKey {
        case tunnelUrl = "tunnel_url"
    }
}

@Observable
public class AgentProtocol: NSObject, URLSessionWebSocketDelegate {
    public static let shared = AgentProtocol()
    
    private var webSocket: URLSessionWebSocketTask?
    public var isConnected = false
    public var currentTrajectory: [TrajectoryStep] = []
    
    // Config parsing to mirror Android logic
    private func getTunnelUrl() -> String {
        // Fallback in case of parsing failure
        let defaultUrl = "wss://antimatter-bridge.cloudflare.net"
        
        guard let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return defaultUrl }
        let configURL = documentsDirectory.appendingPathComponent("config.json")
        
        do {
            let data = try Data(contentsOf: configURL)
            let config = try JSONDecoder().decode(BridgeConfig.self, from: data)
            // Ensure wss:// prefix
            var cleanUrl = config.tunnelUrl
            if cleanUrl.starts(with: "http://") {
                cleanUrl = cleanUrl.replacingOccurrences(of: "http://", with: "ws://")
            } else if cleanUrl.starts(with: "https://") {
                cleanUrl = cleanUrl.replacingOccurrences(of: "https://", with: "wss://")
            }
            return cleanUrl
        } catch {
            print("Failed to read config.json, using default URL. Error: \(error)")
            return defaultUrl
        }
    }
    
    private override init() {
        super.init()
    }
    
    public func connect(token: String) {
        let urlString = getTunnelUrl()
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        
        let session = URLSession(configuration: .default, delegate: self, delegateQueue: OperationQueue())
        webSocket = session.webSocketTask(with: request)
        webSocket?.resume()
        
        schedulePing()
        receiveMessage()
    }
    
    public func disconnect() {
        webSocket?.cancel(with: .goingAway, reason: nil)
        isConnected = false
    }
    
    private func schedulePing() {
        Task {
            try? await Task.sleep(nanoseconds: 15_000_000_000) // 15 seconds
            guard self.isConnected else { return }
            
            webSocket?.sendPing { error in
                if let error = error {
                    print("Ping failed: \(error)")
                    self.disconnect()
                } else {
                    self.schedulePing()
                }
            }
        }
    }
    
    private func receiveMessage() {
        webSocket?.receive { [weak self] result in
            switch result {
            case .failure(let error):
                print("WebSocket error: \(error)")
                self?.isConnected = false
            case .success(let message):
                switch message {
                case .string(let text):
                    self?.handleIncomingText(text)
                case .data(let data):
                    print("Received binary data: \(data)")
                @unknown default:
                    break
                }
                // Keep listening
                self?.receiveMessage()
            }
        }
    }
    
    private func handleIncomingText(_ text: String) {
        // Send logs to Terminal module
        NotificationCenter.default.post(name: Notification.Name("NewTerminalLog"), object: text)
        
        // Parse incoming JSON into TrajectorySteps
        guard let data = text.data(using: .utf8) else { return }
        do {
            let step = try JSONDecoder().decode(TrajectoryStep.self, from: data)
            Task { @MainActor in
                self.currentTrajectory.append(step)
            }
        } catch {
            // Not a TrajectoryStep JSON, ignore silently
        }
    }
    
    public func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        Task { @MainActor in
            self.isConnected = true
        }
    }
    
    public func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        Task { @MainActor in
            self.isConnected = false
        }
    }
}
