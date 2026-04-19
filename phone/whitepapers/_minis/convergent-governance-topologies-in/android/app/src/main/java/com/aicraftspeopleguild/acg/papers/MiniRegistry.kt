package {{ANDROID_PACKAGE}}

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Governance Topology — three sliders name the emergent form. The
 *  paper argues specialist networks keep rediscovering the same four
 *  shapes; set the knobs and see which one you've built. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var centralization by remember { mutableStateOf(0.5f) }   // 0 = flat, 1 = hub
        var veto           by remember { mutableStateOf(0.3f) }   // 0 = majority, 1 = unanimous
        var visibility     by remember { mutableStateOf(0.5f) }   // 0 = back-room, 1 = glass-house

        val form = classify(centralization, veto, visibility)

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("GOVERNANCE TOPOLOGY",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("centralization  ${"%.2f".format(centralization)}", fontFamily = FontFamily.Monospace)
            Slider(centralization, { centralization = it }, valueRange = 0f..1f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Text("veto strength   ${"%.2f".format(veto)}", fontFamily = FontFamily.Monospace)
            Slider(veto, { veto = it }, valueRange = 0f..1f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Text("visibility      ${"%.2f".format(visibility)}", fontFamily = FontFamily.Monospace)
            Slider(visibility, { visibility = it }, valueRange = 0f..1f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            HorizontalDivider()
            Text("emergent form", fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Text(form.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = primary)
            Text(form.blurb, fontSize = 13.sp, lineHeight = 18.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
        }
    }

    private data class Form(val name: String, val blurb: String)

    private fun classify(c: Float, v: Float, vis: Float): Form {
        return when {
            c > 0.7f && v < 0.4f   -> Form("Monarchic Hub",
                "one decider, loose process — fast but brittle; whoever stands next to the hub gets everything")
            c < 0.3f && v > 0.7f   -> Form("Florentine Guild",
                "flat body, strong veto — medieval consensus; hard to move, but hard to corrupt")
            c < 0.3f && v < 0.4f   -> Form("Bazaar",
                "nobody's in charge, majority rules — fast on small things, paralyzed on anything big")
            vis < 0.3f             -> Form("Star Chamber",
                "opaque process regardless of shape — trust erodes; newcomers discover the rules only after breaking them")
            c in 0.3f..0.7f && v > 0.5f -> Form("Committee",
                "moderate hierarchy, high-ish veto — every decision is a settlement")
            else                   -> Form("Federated Specialists",
                "the stable attractor guilds keep rediscovering · domain leads with cross-review + open logs")
        }
    }
}
