package com.saathi.assistant.automation

import com.saathi.assistant.util.newId
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType(val needsAccessibility: Boolean = false) {
    OPEN_APP,
    OPEN_URI,
    OPEN_SETTINGS,
    DIAL,
    BACK(true),
    HOME(true),
    RECENTS(true),
    SCROLL(true),
    CLICK_TEXT(true),
    CLICK_ID(true),
    TYPE_TEXT(true),
    WAIT,
    SCREENSHOT(true),
    SHARE,
    RUN_WORKFLOW
}

/**
 * One step.
 *  - OPEN_APP: target = app key ("whatsapp") or package name
 *  - OPEN_URI: target = http/https/mailto/geo/market/tel URI
 *  - OPEN_SETTINGS: target = settings key ("wifi"), blank = general
 *  - DIAL: target = contact name or number
 *  - SCROLL: target = up/down/left/right
 *  - CLICK_TEXT: target = visible text / content description; "a|b" = alternatives;
 *    value = optional custom confirmation message
 *  - CLICK_ID: target = view id resource name
 *  - TYPE_TEXT: value = text, target = optional field hint
 *  - WAIT: delayMs = duration
 *  - SHARE: value = text
 *  - RUN_WORKFLOW: target = workflow id
 * delayMs is always applied BEFORE the step runs.
 */
@Serializable
data class Action(
    val id: String = newId(),
    val type: ActionType,
    val target: String = "",
    val value: String = "",
    val delayMs: Long = 0,
    val confirmationRequired: Boolean = false,
    val enabled: Boolean = true
)
