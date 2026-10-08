package com.saathi.assistant.command

import com.saathi.assistant.adapters.AdapterRegistry
import com.saathi.assistant.adapters.SendConfirmText
import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.workflow.Workflow

sealed interface InterpretResult {
    data class Plan(val plan: ActionPlan) : InterpretResult
    data class Failure(val error: ParseError) : InterpretResult
}

/**
 * Turns user input into an [ActionPlan]. Version 1 ships only [RuleBasedCommandInterpreter].
 * An optional, future AiCommandInterpreter can implement this same interface; the app is fully
 * usable without it and no API key is ever required or stored in source.
 */
interface CommandInterpreter {
    fun interpret(command: Command): InterpretResult
}

class RuleBasedCommandInterpreter(
    private val parser: CommandParser,
    private val workflows: () -> List<Workflow>,
    private val adapters: AdapterRegistry,
    private val sendConfirm: SendConfirmText
) : CommandInterpreter {

    override fun interpret(command: Command): InterpretResult {
        val triggers = workflows().filter { it.enabled }
            .map { WorkflowTrigger(it.id, it.triggerPhrases + it.name) }
        val p = parser.parse(command.raw, triggers)
        val title = command.raw.trim()
        fun plan(vararg steps: Action) = InterpretResult.Plan(ActionPlan(title, steps.toList()))
        return when (p.type) {
            CommandType.OPEN_APP -> plan(Action(type = ActionType.OPEN_APP, target = p.arg(Args.APP).orEmpty()))
            CommandType.OPEN_SETTINGS -> plan(Action(type = ActionType.OPEN_SETTINGS, target = p.arg(Args.SETTING).orEmpty()))
            CommandType.CALL -> plan(Action(type = ActionType.DIAL, target = p.arg(Args.NUMBER) ?: p.arg(Args.CONTACT).orEmpty()))
            CommandType.SEND_MESSAGE -> {
                val adapter = adapters.forKey(p.arg(Args.APP).orEmpty())
                val built = adapter?.buildSendMessagePlan(p.arg(Args.CONTACT).orEmpty(), p.arg(Args.MESSAGE).orEmpty(), sendConfirm)
                if (built == null) InterpretResult.Failure(ParseError.APP_UNSUPPORTED) else InterpretResult.Plan(built)
            }
            CommandType.BACK -> plan(Action(type = ActionType.BACK))
            CommandType.HOME -> plan(Action(type = ActionType.HOME))
            CommandType.RECENTS -> plan(Action(type = ActionType.RECENTS))
            CommandType.SCREENSHOT -> plan(Action(type = ActionType.SCREENSHOT))
            CommandType.SCROLL -> plan(Action(type = ActionType.SCROLL, target = p.arg(Args.DIRECTION) ?: "down"))
            CommandType.CLICK -> plan(Action(type = ActionType.CLICK_TEXT, target = p.arg(Args.TEXT).orEmpty()))
            CommandType.TYPE -> plan(Action(type = ActionType.TYPE_TEXT, value = p.arg(Args.TEXT).orEmpty()))
            CommandType.SHARE -> plan(Action(type = ActionType.SHARE, value = p.arg(Args.TEXT).orEmpty()))
            CommandType.RUN_WORKFLOW -> plan(Action(type = ActionType.RUN_WORKFLOW, target = p.arg(Args.WORKFLOW_ID).orEmpty()))
            CommandType.UNKNOWN -> InterpretResult.Failure(p.error ?: ParseError.UNRECOGNIZED)
        }
    }
}
