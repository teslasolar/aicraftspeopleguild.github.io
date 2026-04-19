package {{ANDROID_PACKAGE}}

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Konomi · native reference implementation.
 *
 * The Konomi Standard is self-defining: the paper IS its own spec.
 * This app is the native instance of what that paper declares — it
 * fetches the canonical bundle from the teslasolar Pages API and
 * renders every UDT, state machine, and crosswalk the paper defines
 * as first-class Compose UI. Updates to Konomi data flow in without
 * shipping a new APK.
 *
 * Tabs:
 *   Overview    — motto, counts, paper abstract
 *   Base        — 8 Layer-1 UDTs (Identifier, Timestamp, Quality, …)
 *   Standards   — ISA-95, ISA-88, ISA-18.2, ISA-101, OPC-UA,
 *                 Sparkplug, Modbus, KPI — each expandable
 *   Crosswalks  — cross-standard entity mappings
 *   Validate    — paste a JSON doc, see if it's a valid Konomi artifact
 */
private const val API_URL =
    "https://teslasolar.github.io/aicraftspeopleguild.github.io/" +
    "guild/Enterprise/L4/api/konomi.json"

// ────────────────────────────────────────────────────────────────────
// Data model — maps onto the konomi.json bundle.
// ────────────────────────────────────────────────────────────────────
data class KonomiBundle(
    val name: String, val version: String, val motto: String,
    val generatedAt: String, val counts: Map<String, Int>,
    val base: List<Udt>,
    val standards: List<Standard>,
    val crosswalks: List<Crosswalk>,
)

data class Udt(val name: String, val fields: List<Field>, val desc: String)
data class Field(val n: String, val t: String)
data class StateMachine(val name: String, val states: List<String>, val initial: String?, val transitions: List<Transition>)
data class Transition(val from: String, val to: String, val trigger: String?)
data class Standard(
    val sid: String, val id: String, val scope: String, val hierarchy: String,
    val levels: List<String>, val udts: List<Udt>, val stateMachines: List<StateMachine>,
)
data class Crosswalk(val fromStd: String, val toStd: String, val maps: List<CwMap>)
data class CwMap(val from: String, val to: String, val mapping: String, val transform: String)

// ────────────────────────────────────────────────────────────────────
// Parser.
// ────────────────────────────────────────────────────────────────────
fun parseBundle(raw: String): KonomiBundle {
    val root = JSONObject(raw)
    val meta = root.getJSONObject("_meta")
    val counts = meta.getJSONObject("counts").let { c ->
        c.keys().asSequence().associateWith { k -> c.getInt(k) }
    }
    fun parseUdt(o: JSONObject): Udt {
        val fields = mutableListOf<Field>()
        o.optJSONArray("fields")?.let { arr ->
            for (i in 0 until arr.length()) {
                val f = arr.getJSONObject(i)
                val n = f.optString("n").ifBlank { f.optString("name") }
                val t = f.optString("t").ifBlank { f.optString("type") }
                fields.add(Field(n, t))
            }
        }
        val desc = o.optJSONObject("meta")?.optString("desc", "") ?: ""
        return Udt(o.optString("name"), fields, desc)
    }
    fun parseSm(o: JSONObject): StateMachine {
        val states = mutableListOf<String>()
        o.optJSONArray("states")?.let { for (i in 0 until it.length()) states.add(it.getString(i)) }
        val trs = mutableListOf<Transition>()
        o.optJSONArray("transitions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                trs.add(Transition(t.optString("from"), t.optString("to"),
                                   t.optString("trigger").ifBlank { null }))
            }
        }
        return StateMachine(o.optString("name"), states,
            o.optString("initial").ifBlank { null }, trs)
    }
    val base = mutableListOf<Udt>()
    root.optJSONObject("base")?.let { b ->
        for (k in b.keys()) base.add(parseUdt(b.getJSONObject(k)))
    }
    val stds = mutableListOf<Standard>()
    root.optJSONObject("standards")?.let { s ->
        for (sid in s.keys()) {
            val sobj = s.getJSONObject(sid)
            val hdr = sobj.optJSONObject("_std")
            val udts = mutableListOf<Udt>()
            sobj.optJSONArray("udts")?.let { arr -> for (i in 0 until arr.length()) udts.add(parseUdt(arr.getJSONObject(i))) }
            val sms = mutableListOf<StateMachine>()
            sobj.optJSONArray("state_machines")?.let { arr -> for (i in 0 until arr.length()) sms.add(parseSm(arr.getJSONObject(i))) }
            val levels = mutableListOf<String>()
            hdr?.optJSONArray("levels")?.let { arr -> for (i in 0 until arr.length()) levels.add(arr.getString(i)) }
            stds.add(Standard(
                sid           = sid,
                id            = hdr?.optString("id") ?: sid,
                scope         = hdr?.optString("scope") ?: "",
                hierarchy     = hdr?.optString("hierarchy") ?: "",
                levels        = levels,
                udts          = udts,
                stateMachines = sms,
            ))
        }
    }
    val cws = mutableListOf<Crosswalk>()
    root.optJSONArray("crosswalks")?.let { arr ->
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val maps = mutableListOf<CwMap>()
            c.optJSONArray("maps")?.let { ma ->
                for (j in 0 until ma.length()) {
                    val m = ma.getJSONObject(j)
                    maps.add(CwMap(
                        from      = m.optString("from"),
                        to        = m.optString("to"),
                        mapping   = m.optString("mapping", ""),
                        transform = m.optString("transform", ""),
                    ))
                }
            }
            cws.add(Crosswalk(c.optString("from_std"), c.optString("to_std"), maps))
        }
    }
    return KonomiBundle(
        name = meta.optString("name"),  version = meta.optString("version"),
        motto = meta.optString("motto"), generatedAt = meta.optString("generated_at"),
        counts = counts, base = base, standards = stds, crosswalks = cws,
    )
}

// ────────────────────────────────────────────────────────────────────
// Activity + app shell.
// ────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KonomiApp() }
    }
}

@Composable
private fun rememberThemeColor(hex: String): Color = remember(hex) {
    try { Color(hex.toColorInt()) } catch (_: Throwable) { Color(0xFF4A8868) }
}

@Composable
private fun KonomiApp() {
    val primary = rememberThemeColor(stringResource(R.string.theme_color_hex))
    val scheme = darkColorScheme(
        primary       = primary,
        onPrimary     = Color(0xFFFAF6F0),
        background    = Color(0xFF0D1117),
        onBackground  = Color(0xFFE6EDF3),
        surface       = Color(0xFF161B22),
        onSurface     = Color(0xFFE6EDF3),
        surfaceVariant= Color(0xFF21262D),
    )

    MaterialTheme(colorScheme = scheme) {
        var bundle by remember { mutableStateOf<KonomiBundle?>(null) }
        var error  by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            Thread {
                try {
                    val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000; readTimeout = 15_000
                    }
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseBundle(text)
                    Handler(Looper.getMainLooper()).post { bundle = parsed }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { error = e.message ?: "fetch failed" }
                }
            }.start()
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                TopBar(bundle)
                when {
                    error != null -> ErrorBox(error!!)
                    bundle == null -> LoadingBox(primary)
                    else -> Tabs(bundle!!, primary)
                }
            }
        }
    }
}

@Composable
private fun TopBar(b: KonomiBundle?) {
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text("◈ KONOMI · REFERENCE",
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 2.sp)
        Text(stringResource(R.string.paper_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp))
        b?.let {
            Text("v${it.version}  ·  generated ${it.generatedAt}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
    HorizontalDivider(color = Color(0xFF30363D))
}

@Composable
private fun LoadingBox(primary: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = primary)
            Text("fetching konomi.json …",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun ErrorBox(msg: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("could not load bundle", color = Color(0xFFF85149),
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(msg, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp))
            Text(API_URL, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 10.dp))
        }
    }
}

private val TAB_LABELS = listOf("overview", "base", "standards", "crosswalks", "validate")

@Composable
private fun Tabs(b: KonomiBundle, primary: Color) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tab,
            containerColor   = MaterialTheme.colorScheme.background,
            contentColor     = primary,
            edgePadding      = 12.dp,
        ) {
            TAB_LABELS.forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = {
                    Text(label, fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                })
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> OverviewScreen(b, primary)
                1 -> BaseScreen(b, primary)
                2 -> StandardsScreen(b, primary)
                3 -> CrosswalksScreen(b, primary)
                4 -> ValidateScreen(primary)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Screens.
// ────────────────────────────────────────────────────────────────────
@Composable
private fun OverviewScreen(b: KonomiBundle, primary: Color) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
           verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("“${b.motto}”",
            color = primary, fontSize = 15.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        Text(stringResource(R.string.paper_abstract),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontSize = 14.sp, lineHeight = 20.sp)

        val kpis = listOf(
            "base udts"      to b.counts["base_udts"],
            "standards"      to b.counts["standards"],
            "std udts"       to b.counts["udts"],
            "state machines" to b.counts["state_machines"],
            "crosswalks"     to b.counts["crosswalks"],
        )
        FlowRow(kpis, primary)

        Text("standards in this bundle", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
             fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.5.sp,
             modifier = Modifier.padding(top = 8.dp))
        b.standards.forEach { s ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(s.id, color = primary, fontFamily = FontFamily.Monospace,
                         fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (s.scope.isNotBlank()) Text(s.scope,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${s.udts.size} udts", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        if (s.stateMachines.isNotEmpty()) Text("${s.stateMachines.size} sm",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowRow(kpis: List<Pair<String, Int?>>, primary: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        kpis.forEach { (k, v) ->
            Column(Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                   .padding(8.dp)) {
                Text(k.uppercase(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace, fontSize = 8.sp, letterSpacing = 1.sp,
                    maxLines = 1)
                Text(v?.toString() ?: "—", color = primary,
                    fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BaseScreen(b: KonomiBundle, primary: Color) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
               verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("LAYER 1 · BASE UDTs",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text("Atomic reusable types. UUID/PATH/TAG/URN identify; Timestamp carries UTC time; Quality/Value/Range carry industrial data with provenance.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        }
        items(b.base) { u -> UdtCard(u, primary) }
    }
}

@Composable
private fun UdtCard(u: Udt, primary: Color) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(u.name, color = primary, fontFamily = FontFamily.Monospace,
                 fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (u.desc.isNotBlank()) Text(u.desc,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(top = 3.dp))
            if (u.fields.isNotEmpty()) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    u.fields.forEach { f ->
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold)) { append(f.n) }
                            if (f.t.isNotBlank()) {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))) { append(" : ") }
                                withStyle(SpanStyle(color = primary)) { append(f.t) }
                            }
                        }, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardsScreen(b: KonomiBundle, primary: Color) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
               verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(b.standards) { s -> StandardCard(s, primary) }
    }
}

@Composable
private fun StandardCard(s: Standard, primary: Color) {
    var open by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.id, color = primary, fontFamily = FontFamily.Monospace,
                         fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (s.scope.isNotBlank()) Text(s.scope,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                TextButton(onClick = { open = !open }) {
                    Text(if (open) "−" else "+", color = primary, fontSize = 16.sp)
                }
            }
            if (open) {
                if (s.hierarchy.isNotBlank()) Text(s.hierarchy,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontFamily = FontFamily.Monospace, fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 10.dp))
                if (s.levels.isNotEmpty()) {
                    Text("LEVELS", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 10.dp))
                    s.levels.forEach { Text("· $it", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                }
                if (s.udts.isNotEmpty()) {
                    Text("USER-DEFINED TYPES (${s.udts.size})",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.udts.forEach { u -> UdtCard(u, primary) }
                    }
                }
                if (s.stateMachines.isNotEmpty()) {
                    Text("STATE MACHINES (${s.stateMachines.size})",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    s.stateMachines.forEach { sm -> SmCard(sm, primary) }
                }
            }
        }
    }
}

@Composable
private fun SmCard(sm: StateMachine, primary: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))) {
        Column(Modifier.padding(10.dp)) {
            Text(sm.name, color = primary, fontFamily = FontFamily.Monospace,
                 fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sm.states.forEach { st ->
                    val isInit = st == sm.initial
                    Box(Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isInit) primary.copy(alpha = 0.25f) else Color(0xFF161B22))
                        .border(1.dp, if (isInit) primary else Color(0xFF30363D), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(st, color = if (isInit) primary else MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                            fontWeight = if (isInit) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                sm.transitions.forEach { t ->
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = primary, fontWeight = FontWeight.SemiBold)) { append(t.from) }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))) { append(" → ") }
                        withStyle(SpanStyle(color = primary, fontWeight = FontWeight.SemiBold)) { append(t.to) }
                        t.trigger?.let {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))) { append("  ·  ") }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(it) }
                        }
                    }, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
                }
            }
        }
    }
}

@Composable
private fun CrosswalksScreen(b: KonomiBundle, primary: Color) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
               verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(b.crosswalks) { cw -> CwCard(cw, primary) }
    }
}

@Composable
private fun CwCard(cw: Crosswalk, primary: Color) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) { append(cw.fromStd) }
                withStyle(SpanStyle(color = primary)) { append("  →  ") }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) { append(cw.toStd) }
            }, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cw.maps.forEach { m ->
                    val mc = when (m.mapping.lowercase()) {
                        "exact"    -> Color(0xFF3FB950)
                        "partial"  -> Color(0xFFE3B341)
                        "semantic" -> primary
                        else       -> Color(0xFF8B949E)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.from, color = MaterialTheme.colorScheme.onSurface,
                             fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                             fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Box(Modifier.clip(RoundedCornerShape(8.dp))
                            .background(mc.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(m.mapping.uppercase(), color = mc,
                                fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(m.to, color = MaterialTheme.colorScheme.onSurface,
                             fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                             fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                    if (m.transform.isNotBlank()) Text(m.transform,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Validate — structural kind inference + Layer-0 checks.
// ────────────────────────────────────────────────────────────────────
private val SAMPLE_UDT = """
{
  "name": "Pressure",
  "fields": [
    { "n": "v", "t": "REAL" },
    { "n": "q", "t": "Quality" }
  ]
}
""".trimIndent()

private fun inferKind(o: JSONObject): String {
    if (o.has("from_std") && o.has("to_std") && o.has("maps")) return "CROSSWALK"
    if (o.has("states")   && o.has("transitions"))              return "STATE_MACHINE"
    if (o.has("id")       && o.has("scope"))                    return "STD"
    if (o.has("name")     && o.has("fields"))                   return "UDT"
    if (o.has("name")     && o.has("udt"))                      return "ENTITY"
    if (o.has("condition")&& o.has("action"))                   return "RULE"
    return "UNKNOWN"
}

private fun validate(raw: String): Triple<String?, String, List<String>> {
    val errs = mutableListOf<String>()
    val o: JSONObject = try { JSONObject(raw.trim()) }
    catch (e: Exception) { return Triple("PARSE", "parse error", listOf(e.message ?: "invalid JSON")) }
    val kind = inferKind(o)
    if (kind == "UNKNOWN") return Triple(null, "UNKNOWN", listOf("doesn't match any Konomi artifact shape"))

    val required = mapOf(
        "STD" to listOf("id"),
        "UDT" to listOf("name"),
        "CROSSWALK" to listOf("from_std","to_std","maps"),
        "STATE_MACHINE" to listOf("name","states","transitions"),
        "ENTITY" to listOf("name","udt"),
        "RULE" to listOf("id","condition","action"),
    )
    for (k in required[kind] ?: emptyList()) if (!o.has(k)) errs += "missing required field: $k"

    if (kind == "UDT") o.optJSONArray("fields")?.let { arr ->
        for (i in 0 until arr.length()) {
            val f = arr.getJSONObject(i)
            if (!f.has("n") && !f.has("name")) errs += "fields[$i]: missing n/name"
            if (!f.has("t") && !f.has("type")) errs += "fields[$i]: missing t/type"
        }
    }
    if (kind == "STATE_MACHINE") {
        val ss = mutableSetOf<String>()
        o.optJSONArray("states")?.let { for (i in 0 until it.length()) ss.add(it.getString(i)) }
        val init = o.optString("initial")
        if (init.isNotBlank() && init !in ss) errs += "initial state \"$init\" not in states"
        o.optJSONArray("transitions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                val from = t.optString("from"); val to = t.optString("to")
                if (from != "*" && from !in ss) errs += "transitions[$i].from \"$from\" not in states"
                if (to   !in ss)                errs += "transitions[$i].to \"$to\" not in states"
            }
        }
    }
    if (kind == "CROSSWALK") o.optJSONArray("maps")?.let { arr ->
        val valid = setOf("exact","partial","semantic")
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            if (m.optString("from").isBlank()) errs += "maps[$i]: missing from"
            if (m.optString("to").isBlank())   errs += "maps[$i]: missing to"
            val mapping = m.optString("mapping")
            if (mapping.isNotBlank() && mapping.lowercase() !in valid)
                errs += "maps[$i].mapping \"$mapping\" must be exact|partial|semantic"
        }
    }
    return Triple(null, kind, errs)
}

@Composable
private fun ValidateScreen(primary: Color) {
    var input by remember { mutableStateOf(SAMPLE_UDT) }
    val (parseErr, kind, errs) = remember(input) { validate(input) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("VALIDATE · Layer-0 checks",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text("Paste a JSON doc — kind is inferred structurally (STD / UDT / CROSSWALK / STATE_MACHINE / ENTITY / RULE) then checked against Konomi's Layer-0 rules.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontSize = 12.sp, lineHeight = 17.sp)

        OutlinedTextField(
            value = input, onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().height(220.dp),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                  color = MaterialTheme.colorScheme.onSurface),
            label = { Text("JSON", fontSize = 11.sp) },
        )

        val ok = parseErr == null && kind != "UNKNOWN" && errs.isEmpty()
        val color = when {
            parseErr != null                    -> Color(0xFFF85149)
            kind == "UNKNOWN" || errs.isNotEmpty() -> Color(0xFFF85149)
            else                                -> Color(0xFF3FB950)
        }
        Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    when {
                        parseErr != null    -> "✗ parse error"
                        kind == "UNKNOWN"   -> "✗ couldn't infer kind"
                        errs.isEmpty()      -> "✓ valid · $kind"
                        else                -> "✗ $kind · ${errs.size} issue${if (errs.size>1) "s" else ""}"
                    },
                    color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                )
                if (errs.isNotEmpty()) Column(Modifier.padding(top = 6.dp),
                                              verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    errs.forEach {
                        Text("· $it", color = color.copy(alpha = 0.85f),
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
