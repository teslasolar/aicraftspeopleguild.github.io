import SwiftUI

@main
struct PaperApp: App {
    var body: some Scene {
        WindowGroup { PaperView().preferredColorScheme(.dark) }
    }
}

/// All paper fields are pulled from the app's Info.plist so the
/// generator never has to escape Swift string literals for titles
/// that contain quotes, backslashes, etc.
struct PaperView: View {
    private let meta = PaperMeta.load()
    private var primary: Color { Color(hex: meta.themeColorHex) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if !meta.docNumber.isEmpty {
                    Text(meta.docNumber)
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .kerning(2).foregroundStyle(primary)
                }
                Text(meta.title)
                    .font(.system(size: 28, weight: .bold))
                    .lineSpacing(4)
                let byline = [meta.author, meta.date].filter { !$0.isEmpty }.joined(separator: "  ·  ")
                if !byline.isEmpty {
                    Text(byline).font(.callout).foregroundStyle(.secondary)
                }
                Divider()
                if !meta.abstract.isEmpty {
                    Text(meta.abstract)
                        .font(.body).lineSpacing(4)
                        .foregroundStyle(.primary.opacity(0.92))
                }
                if let url = URL(string: meta.paperUrl), !meta.paperUrl.isEmpty {
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

/// Paper metadata baked into Info.plist by the generator.
struct PaperMeta {
    var title, author, date, docNumber, abstract, paperUrl, themeColorHex: String

    static func load() -> PaperMeta {
        let b = Bundle.main.infoDictionary ?? [:]
        func s(_ k: String) -> String { (b[k] as? String) ?? "" }
        return PaperMeta(
            title:          s("ACGPaperTitle"),
            author:         s("ACGPaperAuthor"),
            date:           s("ACGPaperDate"),
            docNumber:      s("ACGPaperDocNumber"),
            abstract:       s("ACGPaperAbstract"),
            paperUrl:       s("ACGPaperUrl"),
            themeColorHex:  s("ACGPaperThemeColor"),
        )
    }
}

// Hex helper — accepts "#RRGGBB" or "#AARRGGBB".
extension Color {
    init(hex: String) {
        var s = hex.trimmingCharacters(in: .whitespaces)
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6 || s.count == 8, let n = UInt64(s, radix: 16) else {
            self = Color(red: 0.10, green: 0.36, blue: 0.30); return
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
