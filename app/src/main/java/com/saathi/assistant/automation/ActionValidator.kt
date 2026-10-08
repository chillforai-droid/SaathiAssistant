package com.saathi.assistant.automation

import com.saathi.assistant.command.ActionPlan
import com.saathi.assistant.command.SettingsKeys

/** Pure validation — used before running plans and when importing workflow JSON. */
object ActionValidator {
    const val MAX_DELAY_MS = 60_000L
    const val MAX_FIELD = 2_000
    const val MAX_STEPS = 100
    private val URI_SCHEMES = setOf("http", "https", "mailto", "geo", "market", "tel")
    private val DIRECTIONS = setOf("up", "down", "left", "right")

    fun isPhoneNumber(s: String): Boolean {
        val t = s.trim()
        if (t.isEmpty() || !t.all { it.isDigit() || it in "+ -()" }) return false
        if (t.indexOf('+') > 0) return false
        return t.count { it.isDigit() } in 3..15
    }

    /** Returns an English technical description of the first problem, or null if valid. */
    fun validate(a: Action): String? {
        if (a.delayMs !in 0..MAX_DELAY_MS) return "delayMs must be between 0 and $MAX_DELAY_MS"
        if (a.target.length > MAX_FIELD || a.value.length > MAX_FIELD) return "target/value too long"
        return when (a.type) {
            ActionType.OPEN_APP -> if (a.target.isBlank()) "OPEN_APP needs a target app" else null
            ActionType.OPEN_URI -> {
                val scheme = a.target.substringBefore(":", "").lowercase()
                if (scheme !in URI_SCHEMES) "OPEN_URI scheme not allowed: '$scheme'" else null
            }
            ActionType.OPEN_SETTINGS ->
                if (a.target.isNotBlank() && a.target.lowercase() !in SettingsKeys.keys) "Unknown settings target '${a.target}'" else null
            ActionType.DIAL -> if (a.target.isBlank()) "DIAL needs a contact or number" else null
            ActionType.CLICK_TEXT, ActionType.CLICK_ID ->
                if (a.target.isBlank()) "${a.type} needs a target" else null
            ActionType.TYPE_TEXT -> if (a.value.isEmpty()) "TYPE_TEXT needs a value" else null
            ActionType.SCROLL -> if (a.target.lowercase() !in DIRECTIONS) "SCROLL target must be up/down/left/right" else null
            ActionType.WAIT -> if (a.delayMs < 1) "WAIT needs delayMs > 0" else null
            ActionType.SHARE -> if (a.value.isBlank()) "SHARE needs content" else null
            ActionType.RUN_WORKFLOW -> if (a.target.isBlank()) "RUN_WORKFLOW needs a workflow id" else null
            ActionType.BACK, ActionType.HOME, ActionType.RECENTS, ActionType.SCREENSHOT -> null
        }
    }

    fun validatePlan(plan: ActionPlan): List<String> {
        if (plan.steps.isEmpty()) return listOf("Plan has no steps")
        if (plan.steps.size > MAX_STEPS) return listOf("Plan has more than $MAX_STEPS steps")
        return plan.steps.mapIndexedNotNull { i, a -> validate(a)?.let { "Step ${i + 1}: $it" } }
    }
}
