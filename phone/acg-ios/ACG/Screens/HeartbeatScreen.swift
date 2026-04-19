import SwiftUI

struct HeartbeatScreen: View {
    @ObservedObject var world: World
    @State private var current: GhTagClient.TagValue?
    @State private var error: String?
    @State private var busy = false
    @State private var bumpResult: String?

    var body: some View {
        VStack(spacing: 16) {
            Text("💓").font(.system(size: 64))
            Text("demo.heartbeat")
                .font(.system(.body, design: .monospaced).bold())
                .foregroundStyle(.green)

            VStack(alignment: .leading, spacing: 6) {
                Text("value").font(.caption2).foregroundStyle(.secondary)
                Text(current?.value ?? "—").font(.system(.title3, design: .monospaced))
                Text("utc").font(.caption2).foregroundStyle(.secondary)
                Text(pretty(current?.value)).font(.system(.body, design: .monospaced))
                if let q = current?.quality {
                    Text("quality: \(q)").font(.caption).foregroundStyle(.secondary)
                }
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(white: 0.1))
            .clipShape(RoundedRectangle(cornerRadius: 8))

            HStack {
                Button("Refresh") { Task { await refresh() } }.buttonStyle(.bordered).disabled(busy)
                Button(world.tokens.hasToken ? "Bump heartbeat" : "Bump (no PAT)") {
                    Task { await bump() }
                }.buttonStyle(.borderedProminent).disabled(busy || !world.tokens.hasToken)
            }
            if busy { ProgressView() }
            if let e = error { Text(e).foregroundStyle(.red) }
            if let r = bumpResult { Text(r).foregroundStyle(.green) }
        }
        .padding()
        .task { await refresh() }
    }

    private func refresh() async {
        busy = true; error = nil
        do { current = try await world.ghTag.readTag("demo.heartbeat") }
        catch { self.error = "\(error)" }
        busy = false
    }

    private func bump() async {
        busy = true; error = nil; bumpResult = nil
        do {
            let epoch = Int(Date().timeIntervalSince1970)
            let outcome = try await world.sync.writeAndSync("demo.heartbeat", value: "\(epoch)",
                                                            type: "Counter",
                                                            description: "bumped from ACG iOS")
            bumpResult = "✓ \(outcome) · \(epoch)"
            try? await Task.sleep(nanoseconds: 500_000_000)
            current = try await world.ghTag.readTag("demo.heartbeat")
        } catch { self.error = "✗ \(error)" }
        busy = false
    }

    private func pretty(_ v: String?) -> String {
        guard let v, let n = TimeInterval(v) else { return v ?? "—" }
        let f = ISO8601DateFormatter()
        return f.string(from: Date(timeIntervalSince1970: n))
    }
}
