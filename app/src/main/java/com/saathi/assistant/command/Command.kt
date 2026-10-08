package com.saathi.assistant.command

import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionValidator

enum class CommandSource { TEXT, VOICE, QUICK_ACTION }

/** Raw user input. */
data class Command(val raw: String, val source: CommandSource = CommandSource.TEXT)

enum class CommandType {
    OPEN_APP, OPEN_SETTINGS, CALL, SEND_MESSAGE, BACK, HOME, RECENTS, SCREENSHOT,
    SCROLL, CLICK, TYPE, SHARE, RUN_WORKFLOW, UNKNOWN
}

/** Machine-readable reasons; the UI maps them to localized text. */
enum class ParseError { EMPTY, NO_CONTACT, NO_MESSAGE, NO_APP, NO_TARGET, NO_TEXT, UNRECOGNIZED, APP_UNSUPPORTED }

object Args {
    const val APP = "app"
    const val SETTING = "setting"
    const val CONTACT = "contact"
    const val NUMBER = "number"
    const val MESSAGE = "message"
    const val TEXT = "text"
    const val DIRECTION = "direction"
    const val WORKFLOW_ID = "workflow_id"
}

/** Result of parsing: intent + arguments, no Android dependencies. */
data class ParsedCommand(
    val raw: String,
    val type: CommandType,
    val args: Map<String, String> = emptyMap(),
    val error: ParseError? = null
) {
    val isKnown: Boolean get() = type != CommandType.UNKNOWN
    fun arg(key: String): String? = args[key]
}

/** Phrases that should start a saved workflow. */
data class WorkflowTrigger(val id: String, val phrases: List<String>)

/** Ordered, validated list of steps ready for the ActionEngine. */
data class ActionPlan(val title: String, val steps: List<Action>) {
    fun validate(): List<String> = ActionValidator.validatePlan(this)
}
