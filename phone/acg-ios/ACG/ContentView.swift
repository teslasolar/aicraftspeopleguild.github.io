import SwiftUI

/// 5-tab bottom nav, mirrors the Android app's ContentView.kt.
struct ContentView: View {
    @ObservedObject var world: World

    var body: some View {
        TabView {
            HomeScreen(world: world)
                .tabItem { Label("Home", systemImage: "house.fill") }
            TagsScreen(world: world)
                .tabItem { Label("Tags", systemImage: "tag.fill") }
            HeartbeatScreen(world: world)
                .tabItem { Label("Heart", systemImage: "waveform.path.ecg") }
            PlantScreen(world: world)
                .tabItem { Label("Plant", systemImage: "dot.radiowaves.left.and.right") }
            SettingsScreen(world: world)
                .tabItem { Label("Set", systemImage: "gear") }
        }
    }
}
