package com.aicraftspeopleguild.acg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.aicraftspeopleguild.acg.data.ApiClient
import com.aicraftspeopleguild.acg.data.GhTagClient
import com.aicraftspeopleguild.acg.data.SensorPlant
import com.aicraftspeopleguild.acg.data.SyncEngine
import com.aicraftspeopleguild.acg.data.TagDb
import com.aicraftspeopleguild.acg.data.TokenStore
import com.aicraftspeopleguild.acg.ui.AcgTheme
import com.aicraftspeopleguild.acg.ui.HeartbeatScreen
import com.aicraftspeopleguild.acg.ui.HomeScreen
import com.aicraftspeopleguild.acg.ui.SettingsScreen
import com.aicraftspeopleguild.acg.ui.TagBrowserScreen
import com.aicraftspeopleguild.acg.ui.organism.PlantScreen
import com.aicraftspeopleguild.acg.udt.UdtRegistry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokens   = TokenStore(this)
        val api      = ApiClient()
        val ghTag    = GhTagClient(tokenProvider = { tokens.getToken() })
        val tagDb    = TagDb(this)
        val sync     = SyncEngine(tagDb, ghTag)
        val plant    = SensorPlant(this)
        val registry = UdtRegistry(this)
        android.util.Log.i("ACG/UDT", "registry loaded: ${registry.countByType()}  total=${registry.instances.size}")

        setContent {
            AcgTheme {
                AcgApp(api = api, ghTag = ghTag, tokens = tokens,
                       tagDb = tagDb, sync = sync, plant = plant,
                       registry = registry)
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home",  Icons.Filled.Home),
    TAGS("Tags",  Icons.Filled.Label),
    HEARTBEAT("Heart", Icons.Filled.MonitorHeart),
    PLANT("Plant", Icons.Filled.Sensors),
    SETTINGS("Set", Icons.Filled.Settings),
}

@Composable
fun AcgApp(
    api: ApiClient, ghTag: GhTagClient, tokens: TokenStore,
    tagDb: TagDb, sync: SyncEngine, plant: SensorPlant,
    registry: UdtRegistry,
) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { padding ->
        val mod = Modifier.fillMaxSize().padding(padding)
        when (tab) {
            Tab.HOME      -> HomeScreen(api, tagDb, sync, mod)
            Tab.TAGS      -> TagBrowserScreen(tagDb, sync, mod)
            Tab.HEARTBEAT -> HeartbeatScreen(ghTag, tokens, sync, mod)
            Tab.PLANT     -> PlantScreen(plant, mod)
            Tab.SETTINGS  -> SettingsScreen(tokens, tagDb, sync, registry, mod)
        }
    }
}
