import Foundation

/// Pulls GitHub-Issues tag DB into the local TagDb and fans each
/// fetched tag out on the Mqtt bus.
final class SyncEngine {
    let tagDb: TagDb
    let ghTag: GhTagClient

    init(tagDb: TagDb, ghTag: GhTagClient) {
        self.tagDb = tagDb
        self.ghTag = ghTag
    }

    struct Result { let pulled: Int; let errors: Int; let durationMs: Int }

    func pullAllTags() async -> Result {
        let start = Date()
        var ok = 0, errs = 0
        do {
            let issues = try await ghTag.listTags()
            for issue in issues {
                let path = String(issue.title.dropFirst("tag:".count))
                if let tv = parseBody(issue.body) {
                    tagDb.upsert(path: path, value: tv, issueNumber: issue.number)
                    Mqtt.shared.publish(topic: "tag.\(path)", value: tv.value ?? "", retained: true, from: "sync")
                    ok += 1
                } else { errs += 1 }
            }
        } catch {
            errs += 1
        }
        tagDb.setMeta("last_full_sync", "\(Int(Date().timeIntervalSince1970 * 1000))")
        return Result(pulled: ok, errors: errs, durationMs: Int(Date().timeIntervalSince(start) * 1000))
    }

    func writeAndSync(_ path: String, value: String, type: String = "String",
                      quality: String = "good", description: String = "written via ACG iOS") async throws -> String {
        let outcome = try await ghTag.writeTag(path, value: value, type: type,
                                               quality: quality, description: description)
        if let tv = try? await ghTag.readTag(path) {
            tagDb.upsert(path: path, value: tv, issueNumber: nil)
            Mqtt.shared.publish(topic: "tag.\(path)", value: tv.value ?? "", retained: true, from: "write")
        }
        return outcome
    }

    private func parseBody(_ body: String?) -> GhTagClient.TagValue? {
        guard var t = body?.trimmingCharacters(in: .whitespacesAndNewlines), !t.isEmpty else { return nil }
        if t.hasPrefix("```") {
            if let idx = t.firstIndex(of: "\n") { t = String(t[t.index(after: idx)...]) }
            if t.hasSuffix("```") { t.removeLast(3) }
            if t.hasPrefix("json") { t.removeFirst(4); t = t.trimmingCharacters(in: .whitespaces) }
        }
        return try? JSONDecoder().decode(GhTagClient.TagValue.self, from: Data(t.utf8))
    }
}
