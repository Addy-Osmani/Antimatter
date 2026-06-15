import Foundation
import CoreNetwork
import SwiftTerm
import Combine
import Observation
import os

@MainActor
@Observable
public class TerminalViewModel {
    public var isConnected = false
    
    private var ptyOutputSubject = PassthroughSubject<Data, Never>()
    private var cancellables = Set<AnyCancellable>()
    private var pingTimer: Timer?
    private let logger = Logger(subsystem: "dev.saifmukhtar.antimatter", category: "TerminalViewModel")

    public init() {
        self.isConnected = AgentProtocol.shared.isConnected
        setupBindings()
    }

    private func setupBindings() {
        // Observe AgentProtocol.isConnected
        withObservationTracking {
            _ = AgentProtocol.shared.isConnected
        } onChange: { [weak self] in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.isConnected = AgentProtocol.shared.isConnected
                if self.isConnected {
                    self.startPing()
                    self.sendPtyStart()
                } else {
                    self.stopPing()
                }
                self.setupBindings() // re-arm observation tracking
            }
        }
        
        // Ensure we handle current state if we start already connected
        if self.isConnected {
            self.startPing()
            self.sendPtyStart()
        }

        NotificationCenter.default.publisher(for: Notification.Name("NewPtyOutput"))
            .compactMap { $0.object as? Data }
            .receive(on: DispatchQueue.main)
            .sink { [weak self] data in
                self?.ptyOutputSubject.send(data)
            }
            .store(in: &cancellables)
    }

    public func onTerminalReady(terminal: TerminalView) {
        // Send initial resize
        let cols = terminal.terminal.cols
        let rows = terminal.terminal.rows
        sendPtyResize(cols: cols, rows: rows)
        
        // Subscribe to output and feed it to SwiftTerm
        ptyOutputSubject
            .receive(on: DispatchQueue.main)
            .sink { [weak terminal] data in
                if let array = [UInt8](data) as? [UInt8] {
                    terminal?.feed(byteArray: array)
                }
            }
            .store(in: &cancellables)
    }

    public func sendPtyInput(data: Data) {
        let base64 = data.base64EncodedString()
        AgentProtocol.shared.sendMessage(["type": "PTY_INPUT", "data": base64])
    }

    public func sendPtyResize(cols: Int, rows: Int) {
        AgentProtocol.shared.sendMessage(["type": "PTY_RESIZE", "cols": cols, "rows": rows])
    }

    private func sendPtyStart() {
        AgentProtocol.shared.sendMessage(["type": "PTY_START"])
    }

    private func startPing() {
        pingTimer?.invalidate()
        pingTimer = Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { _ in
            AgentProtocol.shared.sendMessage(["type": "PTY_PING"])
        }
    }

    private func stopPing() {
        pingTimer?.invalidate()
        pingTimer = nil
    }
}
