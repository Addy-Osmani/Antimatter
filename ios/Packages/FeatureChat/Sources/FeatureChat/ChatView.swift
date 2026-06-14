import SwiftUI
import CoreUI
import CoreNetwork

public struct ChatView: View {
    @Bindable var viewModel: ChatViewModel
    
    public init(viewModel: ChatViewModel = ChatViewModel()) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        ZStack {
            AntimatterTheme.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                HStack {
                    Image(systemName: "atom")
                        .foregroundColor(AntimatterTheme.primary)
                        .font(.title2)
                    Text("Antimatter Agent")
                        .font(.headline)
                        .foregroundColor(AntimatterTheme.textPrimary)
                    
                    Spacer()
                    
                    Button(action: {
                        viewModel.disconnect()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(AntimatterTheme.secondary)
                            .font(.title2)
                    }
                }
                .padding()
                .background(AntimatterTheme.surface)
                
                Divider().background(AntimatterTheme.secondary)
                
                // Chat List with Auto-Scroll
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            if viewModel.trajectories.isEmpty {
                                Text("No active session.")
                                    .foregroundColor(AntimatterTheme.textSecondary)
                                    .padding(.top, 40)
                            } else {
                                ForEach(viewModel.trajectories) { step in
                                    TrajectoryBubble(step: step)
                                        .id(step.id) // Used for auto-scroll
                                }
                            }
                        }
                        .padding()
                    }
                    .onChange(of: viewModel.trajectories.count) {
                        if let lastId = viewModel.trajectories.last?.id {
                            withAnimation {
                                proxy.scrollTo(lastId, anchor: .bottom)
                            }
                        }
                    }
                }
                
                // Input Area
                HStack {
                    TextField("Send a command...", text: $viewModel.inputText)
                        .padding(12)
                        .background(AntimatterTheme.secondary)
                        .cornerRadius(20)
                        .foregroundColor(AntimatterTheme.textPrimary)
                    
                    Button(action: {
                        viewModel.sendMessage()
                    }) {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(viewModel.inputText.isEmpty ? AntimatterTheme.secondary : AntimatterTheme.primary)
                    }
                    .disabled(viewModel.inputText.isEmpty)
                }
                .padding()
                .background(AntimatterTheme.surface)
            }
        }
        .onAppear {
            viewModel.connectToAgent()
        }
    }
}

struct TrajectoryBubble: View {
    let step: TrajectoryStep
    
    var body: some View {
        HStack {
            if step.source == "USER" || step.source == "USER_EXPLICIT" {
                Spacer()
                // Native Markdown Support via LocalizedStringKey
                Text(LocalizedStringKey(step.content ?? ""))
                    .padding(12)
                    .background(AntimatterTheme.primary.opacity(0.2))
                    .foregroundColor(AntimatterTheme.primary)
                    .cornerRadius(16)
                    .cornerRadius(4, corners: [.bottomRight])
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    Text(step.type)
                        .font(.caption2).bold()
                        .foregroundColor(AntimatterTheme.textSecondary)
                    
                    // Native Markdown Support via LocalizedStringKey
                    Text(LocalizedStringKey(step.content ?? "..."))
                        .padding(12)
                        .background(AntimatterTheme.surface)
                        .foregroundColor(AntimatterTheme.textPrimary)
                        .cornerRadius(16)
                        .cornerRadius(4, corners: [.bottomLeft])
                }
                Spacer()
            }
        }
    }
}

// Helper for rounded chat bubbles
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}
