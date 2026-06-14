import Foundation
import CoreData

public struct TrajectoryStep: Codable, Identifiable {
    public let id: Int
    public let source: String
    public let type: String
    public let status: String
    public let content: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "step_index"
        case source, type, status, content
    }
}

public struct AgentMessage: Codable {
    public let type: String
    public let payload: String
}
