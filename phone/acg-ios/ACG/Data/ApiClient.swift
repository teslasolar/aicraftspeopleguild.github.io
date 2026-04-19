import Foundation

/// OkHttp-equivalent. Thin URLSession wrapper around the public L4
/// endpoints on the teslasolar Pages host.
final class ApiClient {
    let baseUrl: String

    init(base: String = "https://teslasolar.github.io/aicraftspeopleguild.github.io") {
        self.baseUrl = base.trimmingCharacters(in: .init(charactersIn: "/")) + "/guild/Enterprise/L4"
    }

    struct Health: Decodable {
        var paperCount: Int?
        var memberCount: Int?
        var lastUpdated: String?
        var apiVersion: String?
    }

    struct EnterpriseSnapshot {
        var papers = 0, members = 0, programs = 0
        var runs = 0, tagEdges = 0, authoredLinks = 0
    }

    func health() async throws -> Health {
        try await getJson(path: "/api/health.json", as: Health.self)
    }

    func enterprise() async throws -> EnterpriseSnapshot {
        let root: [String: Any] = try await getJsonObject(path: "/runtime/tags.json")
        guard let ent = root["enterprise"] as? [String: Any] else { return .init() }
        func pick(_ k: String) -> Int {
            if let n = ent[k] as? Int { return n }
            if let s = ent[k] as? String, let n = Int(s) { return n }
            if let d = ent[k] as? [String: Any] {
                if let n = d["value"] as? Int { return n }
                if let s = d["value"] as? String, let n = Int(s) { return n }
            }
            return 0
        }
        return .init(
            papers: pick("paperCount"), members: pick("memberCount"),
            programs: pick("programCount"), runs: pick("runCount"),
            tagEdges: pick("tagEdges"), authoredLinks: pick("authoredLinks"),
        )
    }

    // MARK: - helpers
    private func getJson<T: Decodable>(path: String, as type: T.Type) async throws -> T {
        let data = try await getData(path: path)
        return try JSONDecoder().decode(T.self, from: data)
    }
    private func getJsonObject(path: String) async throws -> [String: Any] {
        let data = try await getData(path: path)
        return (try JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
    }
    private func getData(path: String) async throws -> Data {
        guard let url = URL(string: baseUrl + path) else { throw URLError(.badURL) }
        let (data, resp) = try await URLSession.shared.data(from: url)
        if let http = resp as? HTTPURLResponse, http.statusCode != 200 {
            throw NSError(domain: "ACG.Api", code: http.statusCode,
                          userInfo: [NSLocalizedDescriptionKey: "HTTP \(http.statusCode) \(url)"])
        }
        return data
    }
}
