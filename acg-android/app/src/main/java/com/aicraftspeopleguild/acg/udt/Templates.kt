package com.aicraftspeopleguild.acg.udt

/**
 * Parameter data classes that mirror the Android UDT templates in
 * assets/udt/. Each class is the typed Kotlin form of one template's
 * `parameters` block. The matching Compose renderer in
 * com.aicraftspeopleguild.acg.ui.{atom,molecule,organism} takes the
 * same-named Params and emits the Compose elements.
 *
 * This mirrors the svg_widget.py + build-*.py pattern on the server:
 * one definition shared between spec (JSON template) and renderer
 * (code), so a new screen is a JSON diff, not a Compose diff.
 *
 * onTap / onClick handlers live outside the Params (templates only
 * describe shape, not behavior) — callers wire them as sibling args
 * to the composables.
 */

enum class ValueColor { TEXT, PRIMARY, ERROR, DIM }

data class StatRowParams(
    val label: String,
    val value: String,
    val valueColor: ValueColor = ValueColor.TEXT,
)

enum class ButtonVariant { PRIMARY, SECONDARY, DANGER }

data class ActionButtonParams(
    val label: String,
    val variant: ButtonVariant = ButtonVariant.SECONDARY,
    val enabled: Boolean = true,
)

enum class Severity { OK, ERR, INFO, WARN }

data class StatusLineParams(
    val text: String,
    val severity: Severity = Severity.INFO,
)

enum class ChipColor { PRIMARY, SECONDARY, ERROR, SURFACE }

data class ChipParams(
    val label: String,
    val color: ChipColor = ChipColor.SURFACE,
)

data class StatCardParams(
    val title: String,
    val rows: List<StatRowParams>,
)

data class ActionRowParams(
    val buttons: List<ActionButtonParams>,
    val busy: Boolean = false,
)

data class StatScreenParams(
    val title: String,
    val subtitle: String? = null,
    val cards: List<StatCardParams> = emptyList(),
    val actionRow: ActionRowParams? = null,
    val status: StatusLineParams? = null,
)
