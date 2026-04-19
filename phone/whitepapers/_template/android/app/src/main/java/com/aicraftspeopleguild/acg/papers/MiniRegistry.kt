package {{ANDROID_PACKAGE}}

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Stub mount point for a paper-specific interactive module (a
 * "mini app"). The generator OVERLAYS this file with a real
 * implementation when the paper's WhitepaperApp UDT instance has
 * `mini_app: "<id>"`. Overlay source lives at
 *   phone/whitepapers/_minis/<id>/android/
 * and must define an object with the same two public members.
 * When no mini is wired, this stub runs and the Try tab stays hidden.
 */
object MiniRegistry {
    fun isAvailable(): Boolean = false

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No interactive layer for this paper yet.",
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                 fontSize = 13.sp)
        }
    }
}
