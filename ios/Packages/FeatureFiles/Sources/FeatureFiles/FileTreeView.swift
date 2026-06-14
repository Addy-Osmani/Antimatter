import SwiftUI
import CoreUI

public struct FileTreeView: View {
    @Bindable var viewModel: FileTreeViewModel
    
    public init(viewModel: FileTreeViewModel = FileTreeViewModel()) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        ZStack {
            AntimatterTheme.background.ignoresSafeArea()
            
            VStack(alignment: .leading, spacing: 0) {
                // Header
                HStack {
                    Image(systemName: "folder.fill")
                        .foregroundColor(AntimatterTheme.primary)
                    Text("Workspace")
                        .font(.headline)
                        .foregroundColor(AntimatterTheme.textPrimary)
                    Spacer()
                }
                .padding()
                .background(AntimatterTheme.surface)
                
                Divider().background(AntimatterTheme.secondary)
                
                List(viewModel.rootFiles, children: \.children) { node in
                    HStack {
                        Image(systemName: node.isDirectory ? "folder.fill" : "doc.fill")
                            .foregroundColor(node.isDirectory ? AntimatterTheme.primary : AntimatterTheme.textSecondary)
                        Text(node.name)
                            .foregroundColor(AntimatterTheme.textPrimary)
                    }
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                }
                .listStyle(PlainListStyle())
                .scrollContentBackground(.hidden)
                .background(AntimatterTheme.background)
            }
        }
    }
}
