package com.aicraftspeopleguild.acg.papers

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 7.83 Hz Pulse — the paper's signature frequency (Schumann
 *  resonance). A visual metronome that beats at 7.83 Hz so you can
 *  see it. No audio — speakers would blow out at that subsonic rate
 *  anyway. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val infinite = rememberInfiniteTransition(label = "schumann")
        val phase by infinite.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween((1000f / 7.83f).toInt(), easing = LinearEasing)),
            label = "phase",
        )
        val r = 60f + 40f * kotlin.math.sin(phase * 2f * kotlin.math.PI).toFloat()

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("7.83 Hz",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("the Earth's Schumann resonance · one pulse every 128 ms",
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2; val cy = size.height / 2
                    drawCircle(primary.copy(alpha = 0.12f), r + 40f, Offset(cx, cy))
                    drawCircle(primary.copy(alpha = 0.32f), r + 20f, Offset(cx, cy))
                    drawCircle(primary, r, Offset(cx, cy))
                }
            }

            Text("counted cycles since load: ${(phase * 1000).toInt()} · " +
                 "(approx every ${(1000f / 7.83f).toInt()} ms)",
                 fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        }
    }
}
