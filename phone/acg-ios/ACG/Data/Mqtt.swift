import Foundation
import Combine

/// Port of the Kotlin `Mqtt` object: topic pub/sub with MQTT-shaped
/// wildcards (`*` one segment, `#` rest of path) and retained-last-value
/// replay to new subscribers. SwiftUI consumes via the published
/// `retained` dictionary.
final class Mqtt: ObservableObject {
    static let shared = Mqtt()
    private init() {}

    struct Message {
        let topic: String
        let value: Any
        let retained: Bool
        let ts: Date
        let from: String?
    }

    @Published private(set) var retained: [String: Message] = [:]
    private var subs: [(id: UUID, pattern: String, regex: NSRegularExpression, fn: (Message) -> Void)] = []

    func subscribe(_ pattern: String, _ fn: @escaping (Message) -> Void) -> UUID {
        let id = UUID()
        let regex = Self.compile(pattern)
        subs.append((id, pattern, regex, fn))
        for (_, m) in retained where Self.matches(regex, topic: m.topic) {
            fn(m)
        }
        return id
    }

    func unsubscribe(_ id: UUID) { subs.removeAll { $0.id == id } }

    func publish(topic: String, value: Any, retained: Bool = true, from: String? = "ios") {
        let m = Message(topic: topic, value: value, retained: retained, ts: Date(), from: from)
        if retained { self.retained[topic] = m }
        for s in subs where Self.matches(s.regex, topic: topic) { s.fn(m) }
    }

    func peek(_ topic: String) -> Message? { retained[topic] }
    var subCount: Int { subs.count }
    var retainedCount: Int { retained.count }

    // MARK: - wildcard matching
    private static func compile(_ pattern: String) -> NSRegularExpression {
        var body = NSRegularExpression.escapedPattern(for: pattern)
        body = body.replacingOccurrences(of: "\\*", with: "[^.]+")
        body = body.replacingOccurrences(of: "\\#", with: ".*")
        return try! NSRegularExpression(pattern: "^\(body)$")
    }
    private static func matches(_ r: NSRegularExpression, topic: String) -> Bool {
        r.firstMatch(in: topic, range: NSRange(topic.startIndex..., in: topic)) != nil
    }
}
