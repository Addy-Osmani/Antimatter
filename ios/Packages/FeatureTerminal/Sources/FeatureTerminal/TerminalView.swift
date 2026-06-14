import SwiftUI
import CoreUI

public struct TerminalView: View {
    @Bindable var viewModel: TerminalViewModel
    
    public init(viewModel: TerminalViewModel = TerminalViewModel()) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        ZStack {
            AntimatterTheme.background.ignoresSafeArea()
            
            VStack(alignment: .leading, spacing: 0) {
                // Header
                HStack {
                    Image(systemName: "terminal")
                        .foregroundColor(AntimatterTheme.primary)
                    Text("Terminal Output")
                        .font(.headline)
                        .foregroundColor(AntimatterTheme.textPrimary)
                    Spacer()
                }
                .padding()
                .background(AntimatterTheme.surface)
                
                Divider().background(AntimatterTheme.secondary)
                
                // Console Output
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 4) {
                        if viewModel.logs.isEmpty {
                            Text("Waiting for connection...")
                                .font(.system(.caption, design: .monospaced))
                                .foregroundColor(AntimatterTheme.textSecondary)
                        } else {
                            ForEach(Array(viewModel.logs.enumerated()), id: \.offset) { index, log in
                                Text("> \(log)")
                                    .font(.system(.caption, design: .monospaced))
                                    .foregroundColor(AntimatterTheme.textPrimary)
                            }
                        }
                    }
                    .padding()
                }
            }
        }
    }
}
