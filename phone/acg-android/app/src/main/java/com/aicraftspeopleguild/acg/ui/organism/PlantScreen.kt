package com.aicraftspeopleguild.acg.ui.organism

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicraftspeopleguild.acg.data.SensorPlant
import com.aicraftspeopleguild.acg.udt.SensorPlantGroups
import com.aicraftspeopleguild.acg.udt.SensorReadingParams
import com.aicraftspeopleguild.acg.ui.molecule.SensorCard

@Composable
fun PlantScreen(plant: SensorPlant, modifier: Modifier = Modifier) {
    // Start listening when the screen enters composition; stop on leave.
    DisposableEffect(plant) {
        plant.start()
        onDispose { plant.stop() }
    }

    val allReadings: Map<String, SensorReadingParams> = plant.readings

    // Bucket every enumerated sensor into groups; any kind not in a
    // named group lands in "Other".
    val grouped: Map<String, List<SensorReadingParams>> = remember(allReadings.keys) {
        val index = SensorPlantGroups.withIndex().toList()
        val acc = LinkedHashMap<String, MutableList<SensorReadingParams>>()
        SensorPlantGroups.forEach { acc[it.title] = mutableListOf() }
        allReadings.values.forEach { r ->
            val group = index.firstOrNull { it.value.sensorKinds.contains(r.kind) }?.value?.title ?: "Other"
            acc.getOrPut(group) { mutableListOf() }.add(r)
        }
        acc.mapValues { it.value.sortedBy { p -> p.kind } }
    }

    val flat: List<Any> = buildList {
        grouped.forEach { (title, rows) ->
            if (rows.isEmpty()) return@forEach
            add(Header(title, rows.size))
            addAll(rows)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(flat) { item ->
            when (item) {
                is Header -> Text(
                    "${item.title.uppercase()}  ·  ${item.count}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                is SensorReadingParams -> SensorCard(item)
            }
        }
    }
}

private data class Header(val title: String, val count: Int)
