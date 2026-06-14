import SwiftUI
import CoreUI

public struct ConnectView: View {
    @Bindable var viewModel: ConnectViewModel
    
    public init(viewModel: ConnectViewModel = ConnectViewModel()) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        ZStack {
            AntimatterTheme.background.ignoresSafeArea()
            
            VStack(spacing: 32) {
                Spacer()
                
                // Logo placeholder
                Circle()
                    .fill(AntimatterTheme.primary.opacity(0.1))
                    .frame(width: 120, height: 120)
                    .overlay(
                        Image(systemName: "atom")
                            .font(.system(size: 60))
                            .foregroundColor(AntimatterTheme.primary)
                    )
                
                VStack(spacing: 8) {
                    Text("Antimatter")
                        .font(.system(.largeTitle, design: .rounded).bold())
                        .foregroundColor(AntimatterTheme.textPrimary)
                    
                    Text("Enter your pairing token to connect to your local agent.")
                        .font(.subheadline)
                        .foregroundColor(AntimatterTheme.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                
                VStack(spacing: 16) {
                    TextField("Pairing Token", text: $viewModel.inputToken)
                        .padding()
                        .background(AntimatterTheme.secondary)
                        .cornerRadius(12)
                        .foregroundColor(AntimatterTheme.textPrimary)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                    
                    if let error = viewModel.errorMessage {
                        Text(error)
                            .foregroundColor(AntimatterTheme.error)
                            .font(.caption)
                    }
                    
                    Button(action: {
                        viewModel.connect()
                    }) {
                        if viewModel.isConnecting {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: AntimatterTheme.background))
                        } else {
                            Text("Connect")
                                .bold()
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AntimatterTheme.primary)
                    .foregroundColor(AntimatterTheme.background)
                    .cornerRadius(12)
                    .disabled(viewModel.inputToken.isEmpty || viewModel.isConnecting)
                }
                .padding(.horizontal, 24)
                
                Spacer()
            }
        }
    }
}

#Preview {
    ConnectView()
}
