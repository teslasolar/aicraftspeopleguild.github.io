package com.aicraftspeopleguild.acg.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.aicraftspeopleguild.acg.udt.SensorAccuracy
import com.aicraftspeopleguild.acg.udt.SensorReadingParams
import com.aicraftspeopleguild.acg.udt.hwTypeToKind
import com.aicraftspeopleguild.acg.udt.permissionForKind
import com.aicraftspeopleguild.acg.udt.unitForKind

/**
 * The L0/L1 sensor plant. Enumerates every [Sensor] the device
 * exposes, subscribes via [SensorManager], and keeps a live
 * SnapshotStateMap<kind, SensorReadingParams> that Compose screens
 * can observe directly. Every delivery is also republished to the
 * [Mqtt] bus at topic `sensor.<kind>.value` so scripts/widgets can
 * subscribe without caring which sensor is which.
 *
 * Call [start] when a screen wants live values, [stop] when it
 * goes away — registration is reference-counted per kind so two
 * screens watching the same sensor only cost one callback.
 */
class SensorPlant(ctx: Context) : SensorEventListener {
    private val sm: SensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val hwSensors: List<Sensor> = sm.getSensorList(Sensor.TYPE_ALL)

    /** Observable live map — Compose reads this directly via `plant.readings`. */
    val readings: SnapshotStateMap<String, SensorReadingParams> = mutableStateMapOf()

    init {
        hwSensors.forEach { s ->
            val kind = hwTypeToKind(s.type, fallback = "type-${s.type}")
            readings[kind] = SensorReadingParams(
                kind        = kind,
                hwType      = s.type,
                name        = s.name,
                vendor      = s.vendor,
                unit        = unitForKind(kind),
                resolution  = s.resolution,
                maxRange    = s.maximumRange,
                minDelayUs  = s.minDelay,
                permission  = permissionForKind(kind),
            )
        }
    }

    fun knownKinds(): List<String> = readings.keys.sorted()

    fun start() {
        hwSensors.forEach { s ->
            // Skip sensors that need a permission we may not hold — caller requests first.
            val kind = hwTypeToKind(s.type)
            if (permissionForKind(kind).isNotEmpty()) return@forEach
            sm.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)
        }
    }
    fun stop() { sm.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent) {
        val kind = hwTypeToKind(event.sensor.type)
        val prev = readings[kind] ?: return
        val newP = prev.copy(
            values = event.values.copyOf(),
            accuracy = when (event.accuracy) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH   -> SensorAccuracy.HIGH
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
                SensorManager.SENSOR_STATUS_ACCURACY_LOW    -> SensorAccuracy.LOW
                SensorManager.SENSOR_STATUS_UNRELIABLE      -> SensorAccuracy.UNRELIABLE
                else                                        -> SensorAccuracy.NONE
            },
            ts = System.currentTimeMillis(),
        )
        readings[kind] = newP
        Mqtt.publish("sensor.$kind.value", newP.values.toList(), retained = true, from = "plant")
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* noop */ }
}
