import Foundation

/// Loads every JSON file in Resources/udt/ at startup. Files whose
/// names end in `.template.json` are UDT type declarations; every
/// other JSON is expected to carry `{udtType, instance, parameters}`
/// and lands in the instance list.
struct UdtRegistry {
    struct Template { let udtType: String; let file: String; let description: String }
    struct Instance {
        let udtType: String
        let instance: String
        let file: String
        let parameters: [String: Any]

        func str(_ key: String) -> String?     { parameters[key] as? String }
        func strList(_ key: String) -> [String] { (parameters[key] as? [String]) ?? [] }
    }

    let templates: [Template]
    let instances: [Instance]

    var typesPresent: [String] { Array(Set(instances.map { $0.udtType })).sorted() }
    var countByType: [String: Int] {
        Dictionary(grouping: instances, by: \.udtType).mapValues { $0.count }
    }

    func templates(of udtType: String) -> [Template] { templates.filter { $0.udtType == udtType } }
    func instances(of udtType: String) -> [Instance] {
        instances.filter { $0.udtType == udtType }.sorted { $0.instance < $1.instance }
    }

    static func load() -> UdtRegistry {
        guard let url = Bundle.main.url(forResource: "udt", withExtension: nil),
              let files = try? FileManager.default.contentsOfDirectory(at: url, includingPropertiesForKeys: nil)
        else { return UdtRegistry(templates: [], instances: []) }

        var tpls: [Template] = []; var insts: [Instance] = []
        for f in files where f.pathExtension == "json" {
            guard let data = try? Data(contentsOf: f),
                  let obj  = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let udtType = obj["udtType"] as? String
            else { continue }

            if f.lastPathComponent.hasSuffix(".template.json") {
                tpls.append(.init(udtType: udtType, file: "udt/\(f.lastPathComponent)",
                                  description: obj["description"] as? String ?? ""))
            } else {
                guard let inst = obj["instance"] as? String,
                      let params = obj["parameters"] as? [String: Any] else { continue }
                insts.append(.init(udtType: udtType, instance: inst,
                                   file: "udt/\(f.lastPathComponent)", parameters: params))
            }
        }
        return UdtRegistry(
            templates: tpls.sorted { $0.udtType < $1.udtType },
            instances: insts.sorted { "\($0.udtType)\($0.instance)" < "\($1.udtType)\($1.instance)" },
        )
    }
}
