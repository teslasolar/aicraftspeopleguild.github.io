import SwiftUI

struct HomeScreen: View {
    @ObservedObject var world: World
    @State private var health: ApiClient.Health?
    @State private var ent: ApiClient.EnterpriseSnapshot?
    @State private var error: String?
    @State private var loading = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("⚒ AI Craftspeople Guild").font(.title2.bold())
                Text("ISA-95 live control-plane").foregroundStyle(.secondary)

                if let e = error { Text(e).foregroundStyle(.red) }

                if let h = health {
                    StatCard(title: "catalog · /api/health.json", rows: [
                        ("papers", "\(h.paperCount ?? 0)"),
                        ("members", "\(h.memberCount ?? 0)"),
                        ("api",     h.apiVersion ?? "—"),
                        ("updated", String((h.lastUpdated ?? "—").prefix(10))),
                    ])
                }
                if let e = ent {
                    StatCard(title: "enterprise · /runtime/tags.json", rows: [
                        ("papers", "\(e.papers)"), ("members", "\(e.members)"),
                        ("programs", "\(e.programs)"), ("runs", "\(e.runs)"),
                        ("tag edges", "\(e.tagEdges)"), ("authored lnks", "\(e.authoredLinks)"),
                    ])
                }
                StatCard(title: "local cache", rows: [
                    ("tags",      "\(world.tagDb.count())"),
                    ("last sync", world.tagDb.getMeta("last_full_sync").flatMap { formatAgo($0) } ?? "never"),
                ])

                HStack(spacing: 8) {
                    Button("Refresh") { Task { await refresh() } }.buttonStyle(.borderedProminent).disabled(loading)
                    Button("Sync tags") { Task { await syncTags() } }.buttonStyle(.bordered).disabled(loading)
                    if loading { ProgressView() }
                }
            }.padding()
        }.task { await refresh() }
    }

    private func refresh() async {
        loading = true; error = nil
        do { async let h = world.api.health(); async let e = world.api.enterprise()
            self.health = try await h; self.ent = try await e
        } catch { self.error = "\(error)" }
        loading = false
    }

    private func syncTags() async {
        loading = true; error = nil
        let r = await world.sync.pullAllTags()
        error = "✓ pulled \(r.pulled) tags · \(r.errors) errors · \(r.durationMs)ms"
        loading = false
    }

    private func formatAgo(_ msStr: String) -> String? {
        guard let ms = Double(msStr) else { return nil }
        let secs = Int((Date().timeIntervalSince1970 * 1000 - ms) / 1000)
        switch secs {
        case ..<60:    return "\(secs)s ago"
        case ..<3600:  return "\(secs / 60)m ago"
        case ..<86400: return "\(secs / 3600)h ago"
        default:       return "\(secs / 86400)d ago"
        }
    }
}

struct StatCard: View {
    let title: String
    let rows: [(String, String)]
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title.uppercased())
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .kerning(2).foregroundStyle(.green)
            ForEach(rows, id: \.0) { k, v in
                HStack { Text(k).foregroundStyle(.secondary); Spacer(); Text(v).bold() }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(white: 0.1))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
