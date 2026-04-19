import SwiftUI

struct SettingsScreen: View {
    @ObservedObject var world: World
    @State private var input: String = ""
    @State private var saveStatus: String?
    @State private var expanded = Set<String>()

    var body: some View {
        Form {
            Section("GitHub token") {
                Text("Required for tag_write + cmd_action. Keychain-stored.").font(.caption).foregroundStyle(.secondary)
                SecureField("GitHub PAT (repo scope)", text: $input)
                HStack {
                    Button("Save") {
                        world.tokens.save(input.trimmingCharacters(in: .whitespacesAndNewlines))
                        saveStatus = world.tokens.hasToken ? "✓ saved" : "cleared"
                    }
                    Button("Clear", role: .destructive) {
                        input = ""; world.tokens.delete(); saveStatus = "cleared"
                    }
                    if let s = saveStatus { Text(s).foregroundStyle(.green) }
                }
            }

            Section("Local tag.db") {
                Text("tags cached: \(world.tagDb.count())  ·  mqtt subs: \(world.mqtt.subCount)  ·  retained: \(world.mqtt.retainedCount)")
                    .font(.caption).foregroundStyle(.secondary)
                Button("Sync now") { Task { _ = await world.sync.pullAllTags() } }
            }

            Section("UDT CATALOG · \(world.registry.instances.count) instances / \(world.registry.typesPresent.count) types") {
                ForEach(Array(world.registry.countByType.keys).sorted(), id: \.self) { ty in
                    DisclosureGroup(isExpanded: Binding(
                        get: { expanded.contains(ty) },
                        set: { on in if on { expanded.insert(ty) } else { expanded.remove(ty) } }
                    )) {
                        ForEach(world.registry.instances(of: ty), id: \.instance) { inst in
                            VStack(alignment: .leading, spacing: 2) {
                                Text(inst.instance).font(.system(.callout, design: .monospaced).bold())
                                if let id = inst.str("id") {
                                    Text("id \(id)").font(.caption2).foregroundStyle(.secondary)
                                }
                                let pubs = inst.strList("publishes"), subs = inst.strList("subscribes")
                                if !pubs.isEmpty || !subs.isEmpty {
                                    Text((pubs.isEmpty ? "" : "pub " + pubs.joined(separator: ",")) +
                                         (subs.isEmpty ? "" : "  ·  sub " + subs.joined(separator: ",")))
                                        .font(.caption2).foregroundStyle(.secondary)
                                }
                            }.padding(.vertical, 2)
                        }
                    } label: {
                        HStack {
                            Text(ty).font(.system(.body, design: .monospaced).bold())
                            Spacer()
                            Text("\(world.registry.countByType[ty] ?? 0)")
                                .font(.system(.caption, design: .monospaced)).foregroundStyle(.secondary)
                        }
                    }
                }
            }

            Section("About") {
                Text("ACG iOS 0.1.0 · SwiftUI · Keychain PAT · Mqtt bus · UDT catalog.").font(.footnote)
                Text("Fork: teslasolar/aicraftspeopleguild.github.io  ·  watches origin too")
                    .font(.footnote).foregroundStyle(.secondary)
            }
        }
    }
}
