import SwiftUI

struct TagsScreen: View {
    @ObservedObject var world: World
    @State private var filter = ""
    @State private var tags: [TagDb.Row] = []
    @State private var loading = false

    var visible: [TagDb.Row] {
        filter.isEmpty ? tags : tags.filter { $0.path.localizedCaseInsensitiveContains(filter) }
    }

    var body: some View {
        VStack {
            TextField("filter path", text: $filter).textFieldStyle(.roundedBorder).padding(.horizontal)
            HStack {
                Text("\(visible.count) of \(tags.count) (local)").font(.caption).foregroundStyle(.secondary)
                Spacer()
                Button("Sync") { Task { await pull() } }.disabled(loading)
            }.padding(.horizontal)
            if tags.isEmpty && !loading {
                VStack(spacing: 12) {
                    Text("No tags cached yet.").foregroundStyle(.secondary)
                    Button("Pull from GitHub") { Task { await pull() } }.buttonStyle(.borderedProminent)
                }.frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(visible) { row in
                    VStack(alignment: .leading) {
                        Text(row.path).font(.system(.body, design: .monospaced)).bold()
                        HStack {
                            if let v = row.value {
                                Text("= \(v.prefix(40))").font(.system(.caption, design: .monospaced))
                                    .foregroundStyle(.green)
                            }
                            if let t = row.type { Text(t).font(.caption2).foregroundStyle(.secondary) }
                            Spacer()
                            if let n = row.issueNumber {
                                Text("#\(n)").font(.caption2).padding(4)
                                    .background(Color(white: 0.2)).clipShape(Capsule())
                            }
                        }
                    }
                }
            }
        }
        .task { tags = world.tagDb.list() }
    }

    private func pull() async {
        loading = true
        _ = await world.sync.pullAllTags()
        tags = world.tagDb.list()
        loading = false
    }
}
