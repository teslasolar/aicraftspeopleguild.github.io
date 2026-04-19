import SwiftUI

/// Entry point. Wires up the long-lived singletons (ApiClient,
/// GhTagClient, TagDb, SyncEngine, SensorPlant, UdtRegistry) once per
/// process and hands them to ContentView.
@main
struct ACGApp: App {
    @StateObject private var world = World()

    var body: some Scene {
        WindowGroup {
            ContentView(world: world)
                .preferredColorScheme(.dark)
        }
    }
}

/// Container for the app's "modules" — the iOS equivalent of the
/// things enumerated in assets/udt/instance-module-*.json.
@MainActor
final class World: ObservableObject {
    let api        = ApiClient()
    let tokens     = TokenStore()
    lazy var ghTag = GhTagClient(repo: "teslasolar/aicraftspeopleguild.github.io",
                                 tokenProvider: { [weak self] in self?.tokens.token })
    let tagDb      = TagDb()
    lazy var sync  = SyncEngine(tagDb: tagDb, ghTag: ghTag)
    let plant      = SensorPlant()
    let mqtt       = Mqtt.shared
    lazy var registry = UdtRegistry.load()

    init() {
        print("ACG/UDT registry: \(registry.countByType) · total=\(registry.instances.count)")
    }
}
