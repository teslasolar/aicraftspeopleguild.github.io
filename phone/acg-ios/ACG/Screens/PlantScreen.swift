import SwiftUI

struct PlantScreen: View {
    @ObservedObject var world: World

    var body: some View {
        List {
            ForEach(world.plant.readings.values.sorted(by: { $0.kind < $1.kind }), id: \.kind) { r in
                SensorRow(r: r)
            }
        }
        .listStyle(.plain)
        .onAppear  { world.plant.start() }
        .onDisappear { world.plant.stop() }
    }
}

struct SensorRow: View {
    let r: SensorPlant.Reading
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(r.kind.replacingOccurrences(of: "_", with: " "))
                    .font(.system(.body, design: .monospaced).bold()).foregroundStyle(.green)
                Spacer()
                Text(r.ts == .distantPast ? "○ idle" : "● live")
                    .font(.caption2).foregroundStyle(.secondary)
            }
            Text(r.name).font(.caption).foregroundStyle(.secondary)
            Text(format(r))
                .font(.system(.callout, design: .monospaced).bold())
        }
        .padding(.vertical, 4)
    }

    private func format(_ r: SensorPlant.Reading) -> String {
        if r.values.isEmpty { return "—" }
        let head = r.values.prefix(3).map { String(format: "%+8.3f", $0) }.joined(separator: "  ")
        return r.unit.isEmpty ? head : "\(head)  \(r.unit)"
    }
}
