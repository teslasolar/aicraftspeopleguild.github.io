import SwiftUI

@main
struct PaperApp: App {
    var body: some Scene {
        WindowGroup { PaperView().preferredColorScheme(.dark) }
    }
}

struct PaperView: View {
    private let primary = Color(hex: "#1A5C4C")

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("ACG-WP-FLYWHEEL")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .kerning(2)
                    .foregroundStyle(primary)
                Text("The Flywheel")
                    .font(.system(size: 28, weight: .bold))
                    .lineSpacing(4)
                Text("AI Craftspeople Guild  ·  2026")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                Divider()
                Text("Compounding returns from craft · a small loop that builds on itself every cycle — write, ship, review, teach, write — and why that loop is the only one that grows without burning its operators out.")
                    .font(.body)
                    .lineSpacing(4)
                    .foregroundStyle(.primary.opacity(0.92))
                if let url = URL(string: "https://teslasolar.github.io/aicraftspeopleguild.github.io/#/whitepapers/flywheel") {
                    Link(destination: url) {
                        Text("Read the full paper ↗")
                            .padding(.horizontal, 16).padding(.vertical, 10)
                            .background(primary).foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }.padding(.top, 8)
                }
                Spacer(minLength: 16)
                Text("part of the ACG whitepaper library  ·  aicraftspeopleguild.github.io")
                    .font(.caption2).foregroundStyle(.secondary)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color(red: 0.05, green: 0.07, blue: 0.09))
    }
}

// Hex helper — accepts "#RRGGBB" or "#AARRGGBB".
extension Color {
    init(hex: String, fallback: Color = Color(red: 0.10, green: 0.36, blue: 0.30)) {
        var s = hex.trimmingCharacters(in: .whitespaces)
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6 || s.count == 8, let n = UInt64(s, radix: 16) else {
            self = fallback; return
        }
        let r, g, b, a: Double
        if s.count == 6 {
            r = Double((n >> 16) & 0xff) / 255
            g = Double((n >> 8)  & 0xff) / 255
            b = Double(n         & 0xff) / 255
            a = 1
        } else {
            a = Double((n >> 24) & 0xff) / 255
            r = Double((n >> 16) & 0xff) / 255
            g = Double((n >> 8)  & 0xff) / 255
            b = Double(n         & 0xff) / 255
        }
        self = Color(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
