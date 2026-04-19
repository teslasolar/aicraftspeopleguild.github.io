import SwiftUI

@main
struct PaperApp: App {
    var body: some Scene {
        WindowGroup { PaperView().preferredColorScheme(.dark) }
    }
}

struct PaperView: View {
    private let primary = Color(hex: "{{THEME_COLOR_HEX}}")

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("{{DOC_NUMBER}}")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .kerning(2)
                    .foregroundStyle(primary)
                Text("{{TITLE}}")
                    .font(.system(size: 28, weight: .bold))
                    .lineSpacing(4)
                Text("{{AUTHOR}}  ·  {{DATE}}")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                Divider()
                Text("{{ABSTRACT}}")
                    .font(.body)
                    .lineSpacing(4)
                    .foregroundStyle(.primary.opacity(0.92))
                if let url = URL(string: "{{PAPER_URL}}") {
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
