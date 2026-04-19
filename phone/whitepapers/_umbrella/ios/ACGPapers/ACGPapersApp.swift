import SwiftUI

@main
struct ACGPapersApp: App {
    var body: some Scene {
        WindowGroup { LibraryView().preferredColorScheme(.dark) }
    }
}

struct Paper: Codable, Identifiable {
    var slug: String
    var title: String
    var author: String
    var date: String
    var doc_number: String
    var abstract: String
    var paper_url: String
    var theme_color_hex: String
    var id: String { slug }
}

extension Bundle {
    static func loadPapers() -> [Paper] {
        guard let url = Bundle.main.url(forResource: "papers", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let arr  = try? JSONDecoder().decode([Paper].self, from: data) else { return [] }
        return arr
    }
}

struct LibraryView: View {
    @State private var papers: [Paper] = Bundle.loadPapers()

    var body: some View {
        NavigationStack {
            List(papers) { p in
                NavigationLink(destination: PaperDetail(p: p)) {
                    HStack(alignment: .top, spacing: 12) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(Color(hex: p.theme_color_hex))
                            .frame(width: 4)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(p.title).font(.headline).foregroundStyle(.white)
                            let by = [p.author, p.date].filter { !$0.isEmpty }.joined(separator: "  ·  ")
                            if !by.isEmpty { Text(by).font(.caption).foregroundStyle(.secondary) }
                            if !p.abstract.isEmpty {
                                Text(p.abstract).font(.caption2).foregroundStyle(.secondary.opacity(0.8))
                                    .lineLimit(2).padding(.top, 2)
                            }
                        }
                    }.padding(.vertical, 4)
                }
            }
            .listStyle(.plain)
            .navigationTitle("ACG Papers")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

struct PaperDetail: View {
    let p: Paper
    private var primary: Color { Color(hex: p.theme_color_hex) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if !p.doc_number.isEmpty {
                    Text(p.doc_number)
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .kerning(2).foregroundStyle(primary)
                }
                Text(p.title).font(.system(size: 28, weight: .bold)).lineSpacing(4)
                let by = [p.author, p.date].filter { !$0.isEmpty }.joined(separator: "  ·  ")
                if !by.isEmpty { Text(by).font(.callout).foregroundStyle(.secondary) }
                Divider()
                if !p.abstract.isEmpty {
                    Text(p.abstract).font(.body).lineSpacing(4)
                        .foregroundStyle(.primary.opacity(0.92))
                }
                if let url = URL(string: p.paper_url), !p.paper_url.isEmpty {
                    Link(destination: url) {
                        Text("Read the full paper ↗")
                            .padding(.horizontal, 16).padding(.vertical, 10)
                            .background(primary).foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }.padding(.top, 8)
                }
                Text("aicraftspeopleguild.github.io  ·  #/whitepapers/\(p.slug)")
                    .font(.system(size: 10, design: .monospaced))
                    .foregroundStyle(.secondary.opacity(0.6))
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color(red: 0.05, green: 0.07, blue: 0.09))
        .navigationBarTitleDisplayMode(.inline)
    }
}

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
