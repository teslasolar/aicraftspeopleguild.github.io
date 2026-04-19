package com.aicraftspeopleguild.acg.papers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Umbrella app · bundles every WhitepaperApp UDT instance into one
 * install. Reads assets/papers.json (populated by
 * phone/whitepapers/_generator/build-umbrella.py), renders them as a
 * LazyColumn, and taps into a hero detail screen built from the same
 * shape as each per-paper app.
 */
@Serializable
data class Paper(
    val slug: String,
    val title: String,
    val author: String = "",
    val date: String = "",
    val doc_number: String = "",
    val abstract: String = "",
    val paper_url: String = "",
    val theme_color_hex: String = "#1A5C4C",
)

private val CHARTER = Color(0xFF1A5C4C)
private val INK     = Color(0xFF0D1117)
private val SURFACE = Color(0xFF161B22)
private val TEXT    = Color(0xFFE6EDF3)
private val DIM     = Color(0xFF8B949E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val papers: List<Paper> = try {
            assets.open("papers.json").bufferedReader().use {
                Json { ignoreUnknownKeys = true }.decodeFromString(it.readText())
            }
        } catch (e: Exception) { emptyList() }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = CHARTER, background = INK, surface = SURFACE,
                onPrimary = Color.White, onBackground = TEXT, onSurface = TEXT,
            )) {
                var selected by rememberSaveable(stateSaver = PaperSaver) { mutableStateOf<Paper?>(null) }
                Surface(color = INK) {
                    if (selected == null) Library(papers) { selected = it }
                    else {
                        BackHandler { selected = null }
                        Detail(selected!!) { selected = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun Library(papers: List<Paper>, onOpen: (Paper) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("⚒ ACG PAPERS",
             fontFamily = FontFamily.Monospace,
             color = CHARTER, fontSize = 11.sp, letterSpacing = 2.sp,
             fontWeight = FontWeight.Bold)
        Text("${papers.size} whitepapers · tap to read",
             color = DIM, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                   contentPadding = PaddingValues(bottom = 24.dp)) {
            items(papers, key = { it.slug }) { p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SURFACE),
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(p) },
                ) {
                    Row(Modifier.padding(14.dp)) {
                        // Color tag strip
                        Box(Modifier
                            .width(4.dp).height(54.dp)
                            .background(parseColor(p.theme_color_hex))
                            .align(Alignment.CenterVertically))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.title,
                                 fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                 lineHeight = 20.sp)
                            val by = listOf(p.author, p.date)
                                .filter { it.isNotBlank() }.joinToString("  ·  ")
                            if (by.isNotBlank())
                                Text(by, color = DIM, fontSize = 11.sp)
                            if (p.abstract.isNotBlank())
                                Text(p.abstract,
                                     color = TEXT.copy(alpha = 0.78f),
                                     fontSize = 12.sp, lineHeight = 16.sp,
                                     maxLines = 2,
                                     modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Detail(p: Paper, onBack: () -> Unit) {
    val primary = parseColor(p.theme_color_hex)
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            TextButton(onClick = onBack) { Text("← back", color = primary) }
        }
        if (p.doc_number.isNotBlank()) {
            Text(p.doc_number,
                 fontFamily = FontFamily.Monospace,
                 color = primary, fontSize = 11.sp, letterSpacing = 2.sp,
                 fontWeight = FontWeight.Bold)
        }
        Text(p.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
        val by = listOf(p.author, p.date).filter { it.isNotBlank() }.joinToString("  ·  ")
        if (by.isNotBlank()) Text(by, color = DIM, fontSize = 13.sp)
        HorizontalDivider()
        if (p.abstract.isNotBlank())
            Text(p.abstract,
                 fontSize = 15.sp, lineHeight = 22.sp,
                 color = TEXT.copy(alpha = 0.88f))
        Spacer(Modifier.height(8.dp))
        if (p.paper_url.isNotBlank()) {
            Button(onClick = {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.paper_url)))
            }, colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                Text("Read the full paper ↗")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("aicraftspeopleguild.github.io  ·  #/whitepapers/${p.slug}",
             color = DIM.copy(alpha = 0.5f), fontSize = 10.sp,
             fontFamily = FontFamily.Monospace)
    }
}

private fun parseColor(hex: String): Color =
    try { Color(hex.toColorInt()) } catch (_: Throwable) { CHARTER }

// ── saveable for selected paper across config change ──
private val PaperSaver = androidx.compose.runtime.saveable.Saver<Paper?, Map<String, String>>(
    save = { p -> p?.let {
        mapOf("slug" to it.slug, "title" to it.title, "author" to it.author,
              "date" to it.date, "doc_number" to it.doc_number,
              "abstract" to it.abstract, "paper_url" to it.paper_url,
              "theme_color_hex" to it.theme_color_hex)
    } ?: emptyMap() },
    restore = { m -> m["slug"]?.let { slug -> Paper(
        slug = slug,
        title = m["title"] ?: "",
        author = m["author"] ?: "",
        date = m["date"] ?: "",
        doc_number = m["doc_number"] ?: "",
        abstract = m["abstract"] ?: "",
        paper_url = m["paper_url"] ?: "",
        theme_color_hex = m["theme_color_hex"] ?: "#1A5C4C",
    ) } },
)

