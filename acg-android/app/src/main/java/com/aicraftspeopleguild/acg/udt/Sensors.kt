package com.aicraftspeopleguild.acg.udt

/**
 * Typed mirrors of the L0/L1 sensor plant templates.
 */

enum class SensorAccuracy { HIGH, MEDIUM, LOW, UNRELIABLE, NONE }

data class SensorReadingParams(
    val kind: String,
    val hwType: Int,
    val name: String,
    val vendor: String,
    val unit: String = "",
    val resolution: Float = 0f,
    val maxRange: Float = 0f,
    val minDelayUs: Int = 0,
    val permission: String = "",
    val values: FloatArray = FloatArray(0),
    val accuracy: SensorAccuracy = SensorAccuracy.NONE,
    val ts: Long = 0L,
)

data class SensorGroup(val title: String, val sensorKinds: List<String>)

/** Default grouping from organism-sensorplant.template.json. */
val SensorPlantGroups: List<SensorGroup> = listOf(
    SensorGroup("Motion",      listOf("accelerometer","linear_acceleration","gravity","gyroscope","gyroscope_uncalibrated","rotation_vector","game_rotation_vector","significant_motion","step_counter","step_detector")),
    SensorGroup("Orientation", listOf("magnetic_field","magnetic_field_uncalibrated","geomagnetic_rotation_vector","orientation")),
    SensorGroup("Environment", listOf("light","proximity","pressure","ambient_temperature","relative_humidity")),
    SensorGroup("Health",      listOf("heart_rate","heart_beat","hinge_angle")),
    SensorGroup("Other",       emptyList()),
)

/** Map android.hardware.Sensor.TYPE_* → our stable kebab-case slug. */
fun hwTypeToKind(hwType: Int, fallback: String = "other"): String = when (hwType) {
    1  -> "accelerometer"
    2  -> "magnetic_field"
    3  -> "orientation"
    4  -> "gyroscope"
    5  -> "light"
    6  -> "pressure"
    7  -> "temperature"
    8  -> "proximity"
    9  -> "gravity"
    10 -> "linear_acceleration"
    11 -> "rotation_vector"
    12 -> "relative_humidity"
    13 -> "ambient_temperature"
    14 -> "magnetic_field_uncalibrated"
    15 -> "game_rotation_vector"
    16 -> "gyroscope_uncalibrated"
    17 -> "significant_motion"
    18 -> "step_detector"
    19 -> "step_counter"
    20 -> "geomagnetic_rotation_vector"
    21 -> "heart_rate"
    22 -> "tilt_detector"
    23 -> "wake_gesture"
    24 -> "glance_gesture"
    25 -> "pick_up_gesture"
    26 -> "wrist_tilt_gesture"
    27 -> "device_orientation"
    28 -> "pose_6dof"
    29 -> "stationary_detect"
    30 -> "motion_detect"
    31 -> "heart_beat"
    34 -> "accelerometer_uncalibrated"
    36 -> "hinge_angle"
    else -> fallback
}

fun unitForKind(kind: String): String = when (kind) {
    "accelerometer","linear_acceleration","gravity" -> "m/s²"
    "gyroscope","gyroscope_uncalibrated"            -> "rad/s"
    "magnetic_field","magnetic_field_uncalibrated"  -> "µT"
    "orientation"                                   -> "°"
    "light"                                         -> "lx"
    "proximity"                                     -> "cm"
    "pressure"                                      -> "hPa"
    "ambient_temperature","temperature"             -> "°C"
    "relative_humidity"                             -> "%"
    "step_counter","step_detector"                  -> "steps"
    "heart_rate","heart_beat"                       -> "bpm"
    "hinge_angle"                                   -> "°"
    else                                            -> ""
}

fun permissionForKind(kind: String): String = when (kind) {
    "heart_rate","heart_beat"       -> "android.permission.BODY_SENSORS"
    "step_counter","step_detector"  -> "android.permission.ACTIVITY_RECOGNITION"
    else                            -> ""
}
