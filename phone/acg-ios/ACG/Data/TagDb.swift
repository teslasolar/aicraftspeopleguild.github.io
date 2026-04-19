import Foundation

/// v0.1: JSON-file backed tag cache. Same DAO shape as the Kotlin
/// TagDb but without an SQLite dependency yet — ships zero third-party
/// libraries. Swap in GRDB when we start needing indexes.
final class TagDb {
    struct Row: Codable, Identifiable {
        var id: String { path }
        let path: String
        let value: String?
        let quality: String?
        let type: String?
        let issueNumber: Int?
        let updatedAt: String?
        let pulledAt: TimeInterval
    }

    private let store: URL
    private var rows: [String: Row] = [:]
    private var meta: [String: String] = [:]

    init() {
        let docs = try! FileManager.default.url(
            for: .applicationSupportDirectory, in: .userDomainMask,
            appropriateFor: nil, create: true)
        let dir  = docs.appendingPathComponent("acg-tags", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        self.store = dir.appendingPathComponent("store.json")
        load()
    }

    func upsert(path: String, value: GhTagClient.TagValue, issueNumber: Int?) {
        rows[path] = Row(
            path: path, value: value.value, quality: value.quality,
            type: value.type, issueNumber: issueNumber, updatedAt: value.updated_at,
            pulledAt: Date().timeIntervalSince1970,
        )
        persist()
    }

    func list(prefix: String = "") -> [Row] {
        rows.values
            .filter { prefix.isEmpty || $0.path.hasPrefix(prefix) }
            .sorted { $0.path < $1.path }
    }

    func read(_ path: String) -> Row? { rows[path] }
    func count() -> Int { rows.count }

    func setMeta(_ key: String, _ value: String?) {
        if let v = value { meta[key] = v } else { meta.removeValue(forKey: key) }
        persist()
    }
    func getMeta(_ key: String) -> String? { meta[key] }

    // MARK: - persistence
    private struct File: Codable { var rows: [Row]; var meta: [String: String] }
    private func persist() {
        let file = File(rows: Array(rows.values), meta: meta)
        if let data = try? JSONEncoder().encode(file) {
            try? data.write(to: store, options: .atomic)
        }
    }
    private func load() {
        guard let data = try? Data(contentsOf: store),
              let file = try? JSONDecoder().decode(File.self, from: data) else { return }
        self.rows = Dictionary(uniqueKeysWithValues: file.rows.map { ($0.path, $0) })
        self.meta = file.meta
    }
}
