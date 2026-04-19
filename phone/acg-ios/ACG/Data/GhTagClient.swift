import Foundation

/// List / read / write `label:tag` issues on a GitHub repo. Anonymous
/// reads; writes need a PAT. Same contract as the Kotlin GhTagClient.
final class GhTagClient {
    let repo: String
    let tokenProvider: () -> String?

    init(repo: String = "teslasolar/aicraftspeopleguild.github.io",
         tokenProvider: @escaping () -> String? = { nil }) {
        self.repo = repo
        self.tokenProvider = tokenProvider
    }

    struct TagIssue: Decodable {
        let number: Int
        let title: String
        let body: String?
        let state: String
        let updated_at: String?
        let comments: Int
    }

    struct TagValue: Codable {
        var value: String?
        var quality: String?
        var type: String?
        var description: String?
        var updated_at: String?
    }

    func listTags() async throws -> [TagIssue] {
        var out: [TagIssue] = []
        var page = 1
        while page < 10 {
            let batch: [TagIssue] = try await get("/repos/\(repo)/issues?labels=tag&state=open&per_page=100&page=\(page)")
            out += batch.filter { $0.title.hasPrefix("tag:") }
            if batch.count < 100 { break }
            page += 1
        }
        return out
    }

    func readTag(_ path: String) async throws -> TagValue? {
        let issues = try await listTags()
        guard let issue = issues.first(where: { $0.title == "tag:\(path)" }) else { return nil }
        return parseBody(issue.body)
    }

    func writeTag(_ path: String, value: String, type: String = "String",
                  quality: String = "good", description: String = "written via ACG iOS") async throws -> String {
        guard let tok = tokenProvider() else {
            throw NSError(domain: "ACG.GhTag", code: 401,
                          userInfo: [NSLocalizedDescriptionKey: "no GitHub token configured"])
        }
        let iso = ISO8601DateFormatter().string(from: Date())
        let tv = TagValue(value: value, quality: quality, type: type, description: description, updated_at: iso)
        let bodyJson = try String(data: JSONEncoder().encode(tv), encoding: .utf8) ?? ""

        let existing = try await allIssues(label: "tag").first { $0.title == "tag:\(path)" }
        if let ex = existing {
            _ = try await request("PATCH", "/repos/\(repo)/issues/\(ex.number)",
                                  body: ["body": bodyJson], token: tok)
            _ = try await request("POST",  "/repos/\(repo)/issues/\(ex.number)/comments",
                                  body: ["body": bodyJson], token: tok)
            return "updated"
        } else {
            _ = try await request("POST", "/repos/\(repo)/issues",
                                  body: ["title": "tag:\(path)", "body": bodyJson, "labels": ["tag"]],
                                  token: tok)
            return "created"
        }
    }

    // MARK: - helpers
    private func allIssues(label: String) async throws -> [TagIssue] {
        var out: [TagIssue] = []
        for state in ["open", "closed"] {
            var page = 1
            while page < 10 {
                let batch: [TagIssue] = (try? await get("/repos/\(repo)/issues?labels=\(label)&state=\(state)&per_page=100&page=\(page)")) ?? []
                out += batch.filter { $0.title.hasPrefix("tag:") }
                if batch.count < 100 { break }
                page += 1
            }
        }
        return out
    }

    private func parseBody(_ body: String?) -> TagValue? {
        guard var t = body?.trimmingCharacters(in: .whitespacesAndNewlines), !t.isEmpty else { return nil }
        if t.hasPrefix("```") {
            if let idx = t.firstIndex(of: "\n") { t = String(t[t.index(after: idx)...]) }
            if t.hasSuffix("```") { t.removeLast(3) }
            if t.hasPrefix("json") { t.removeFirst(4); t = t.trimmingCharacters(in: .whitespaces) }
        }
        return try? JSONDecoder().decode(TagValue.self, from: Data(t.utf8))
    }

    private func get<T: Decodable>(_ path: String) async throws -> T {
        let data = try await request("GET", path, body: nil, token: nil)
        return try JSONDecoder().decode(T.self, from: data)
    }

    @discardableResult
    private func request(_ method: String, _ path: String,
                         body: [String: Any]?, token: String?) async throws -> Data {
        guard let url = URL(string: "https://api.github.com" + path) else { throw URLError(.badURL) }
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
        req.setValue("acg-ios/0.1", forHTTPHeaderField: "User-Agent")
        if let token { req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        if let body {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.httpBody = try JSONSerialization.data(withJSONObject: body)
        }
        let (data, resp) = try await URLSession.shared.data(for: req)
        if let http = resp as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw NSError(domain: "ACG.GhTag", code: http.statusCode,
                          userInfo: [NSLocalizedDescriptionKey: "HTTP \(http.statusCode) \(path)"])
        }
        return data
    }
}
