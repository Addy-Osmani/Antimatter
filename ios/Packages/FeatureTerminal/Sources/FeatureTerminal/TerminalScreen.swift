import SwiftUI
import SwiftTerm
import LocalAuthentication

public struct TerminalScreen: View {
    @State private var viewModel = TerminalViewModel()
    @Environment(\.scenePhase) private var scenePhase
    @State private var isUnlocked = false
    @State private var authError: String? = nil

    public init() {}

    public var body: some View {
        ZStack {
            if isUnlocked {
                SwiftTermView(viewModel: viewModel)
                    .edgesIgnoringSafeArea(.bottom)
            } else {
                Color.black.edgesIgnoringSafeArea(.all)
                VStack(spacing: 20) {
                    Image(systemName: "terminal.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.white)
                    Text("Terminal Locked")
                        .font(.title)
                        .foregroundColor(.white)
                    
                    if let error = authError {
                        Text(error)
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                            .padding()
                    }
                    
                    Button(action: authenticate) {
                        Text("Authenticate to Access Terminal")
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                }
            }
        }
        .onAppear {
            if !isUnlocked {
                authenticate()
            }
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .background {
                // Lock the terminal when app goes to background
                isUnlocked = false
            } else if newPhase == .active && !isUnlocked {
                // Prompt authentication when returning to foreground
                authenticate()
            }
        }
    }

    private func authenticate() {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            let reason = "Unlock Antimatter Terminal"

            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, authenticationError in
                DispatchQueue.main.async {
                    if success {
                        self.isUnlocked = true
                    } else {
                        self.authError = authenticationError?.localizedDescription ?? "Failed to authenticate."
                    }
                }
            }
        } else {
            // Fallback: No biometrics, allow device passcode
            if context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) {
                context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Unlock Antimatter Terminal.") { success, authenticationError in
                    DispatchQueue.main.async {
                        if success {
                            self.isUnlocked = true
                        } else {
                            self.authError = authenticationError?.localizedDescription ?? "Failed to authenticate."
                        }
                    }
                }
            } else {
                // No passcode set
                self.authError = "Device has no passcode or biometrics configured. Please secure your device."
            }
        }
    }
}

struct SwiftTermView: UIViewRepresentable {
    let viewModel: TerminalViewModel

    func makeUIView(context: Context) -> TerminalView {
        let terminalView = TerminalView()
        terminalView.terminalDelegate = context.coordinator
        
        // Notify viewModel
        viewModel.onTerminalReady(terminal: terminalView)
        return terminalView
    }

    func updateUIView(_ uiView: TerminalView, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(viewModel: viewModel)
    }

    class Coordinator: NSObject, TerminalViewDelegate {
        let viewModel: TerminalViewModel

        init(viewModel: TerminalViewModel) {
            self.viewModel = viewModel
        }

        func sizeChanged(source: TerminalView, newCols: Int, newRows: Int) {
            viewModel.sendPtyResize(cols: newCols, rows: newRows)
        }

        func send(source: TerminalView, data: ArraySlice<UInt8>) {
            let nsData = Data(data)
            viewModel.sendPtyInput(data: nsData)
        }

        func setTerminalTitle(source: TerminalView, title: String) {
            viewModel.terminalTitle = title
        }
        
        func hostCurrentDirectoryUpdate(source: TerminalView, directory: String?) {}
        func scrolled(source: TerminalView, position: Double) {}
        func requestOpenLink(source: TerminalView, link: String, params: [String : String]) {}
        func bell(source: TerminalView) {}
        func clipboardCopy(source: TerminalView, content: Data) {}
    }
}
