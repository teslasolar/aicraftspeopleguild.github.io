package com.aicraftspeopleguild.acg.ui.organism

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicraftspeopleguild.acg.udt.UdtRegistry

/**
 * Renders every loaded UDT instance grouped by type, expandable per
 * group. Lives inside Settings for now; pulls straight from the
 * app-local UdtRegistry (asset JSON) — no network.
 */
@Composable
fun CatalogSection(registry: UdtRegistry, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(setOf<String>()) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "UDT CATALOG  ·  ${registry.instances.size} instances / ${registry.typesPresent().size} types",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        registry.countByType().forEach { (udtType, count) ->
            val isOpen = udtType in expanded
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().clickable {
                    expanded = if (isOpen) expanded - udtType else expanded + udtType
                },
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            udtType,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "$count  ${if (isOpen) "▾" else "▸"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                    AnimatedVisibility(visible = isOpen) {
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            registry.instances(udtType).forEach { inst ->
                                InstanceRow(inst)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstanceRow(inst: UdtRegistry.Instance) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            inst.instance,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        // Pick a few high-signal params to show, whichever exist.
        val line1 = listOfNotNull(
            inst.str("id")?.let     { "id $it" },
            inst.str("klass")?.let  { "· " + it.substringAfterLast('.') },
            inst.str("layer")?.let  { "· L$it" },
            inst.str("url")?.let    { "· ${it.take(48)}" },
        ).joinToString(" ")
        if (line1.isNotBlank()) {
            Text(line1, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        val pubs = inst.strList("publishes")
        val subs = inst.strList("subscribes")
        if (pubs.isNotEmpty() || subs.isNotEmpty()) {
            Text(
                buildString {
                    if (pubs.isNotEmpty()) append("pub ").append(pubs.joinToString(","))
                    if (subs.isNotEmpty()) { if (isNotEmpty()) append("  ·  "); append("sub ").append(subs.joinToString(",")) }
                },
                fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
        inst.str("description")?.takeIf { it.isNotBlank() }?.let {
            Text(it, fontSize = 10.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
    }
}
