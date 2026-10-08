package com.saathi.assistant.automation

import android.content.Context
import com.saathi.assistant.R
import com.saathi.assistant.accessibility.AccessibilityActionExecutor
import com.saathi.assistant.accessibility.AccessibilityStateManager
import com.saathi.assistant.adapters.AdapterEnv
import com.saathi.assistant.adapters.AdapterRegistry
import com.saathi.assistant.command.ActionPlan
import com.saathi.assistant.data.ContactResolver
import com.saathi.assistant.data.SettingsStore
import com.saathi.assistant.safety.SafetyClassifier
import com.saathi.assistant.safety.SafetyLevel
import com.saathi.assistant.util.str
import com.saathi.assistant.workflow.Workflow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Executes validated plans step by step with: validation, safety classification, user
 * confirmation, per-step timeout, retry limit, cancellation (cancel the calling coroutine),
 * a live state and an execution log. It never fakes success: a failed step stops the run.
 */
class ActionEngine(
    private val context: Context,
    private val launcher: AppLauncher,
    private val contacts: ContactResolver,
    private val adapters: AdapterRegistry,
    private val workflows: () -> List<Workflow>,
    private val settings: SettingsStore
) {
    sealed interface State {
        data object Idle : State
        data class Running(val title: String, val index: Int, val total: Int, val description: String) : State
    }

    data class LogEntry(val timeMs: Long, val description: String, val ok: Boolean, val message: String)
    data class RunResult(val ok: Boolean, val message: String)

    private val executor = AccessibilityActionExecutor(context)
    private val env = AdapterEnv(context, launcher, executor)

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    /** Run a plan. Cancel by cancelling the coroutine that calls this. */
    suspend fun run(plan: ActionPlan, ui: UserInteraction): RunResult {
        _log.value = emptyList()
        try {
            return runPlan(plan, ui, 0)
        } catch (e: CancellationException) {
            addLog(context.str(R.string.msg_run_cancelled), false, "")
            throw e
        } finally {
            _state.value = State.Idle
        }
    }

    private suspend fun runPlan(plan: ActionPlan, ui: UserInteraction, depth: Int): RunResult {
        val issues = plan.validate()
        if (issues.isNotEmpty()) return RunResult(false, context.str(R.string.msg_invalid_plan, issues.first()))
        val steps = plan.steps.filter { it.enabled }
        for ((i, action) in steps.withIndex()) {
            val desc = ActionDescriber.describe(context, action)
            _state.value = State.Running(plan.title, i + 1, steps.size, desc)
            if (action.delayMs > 0) delay(action.delayMs)
            val r = execute(action, ui, depth)
            addLog(desc, r.ok, r.message)
            if (!r.ok) return RunResult(false, r.message)
        }
        return RunResult(true, context.str(R.string.msg_done))
    }

    private suspend fun execute(action: Action, ui: UserInteraction, depth: Int): StepResult {
        val verdict = SafetyClassifier.classify(action)
        if (verdict.level == SafetyLevel.BLOCK) {
            return StepResult.fail(context.str(R.string.msg_blocked, context.str(verdict.reason.resId())))
        }
        return when (action.type) {
            ActionType.DIAL -> dial(action, ui)
            ActionType.RUN_WORKFLOW -> nested(action, ui, depth)
            else -> {
                if (verdict.level == SafetyLevel.CONFIRM) {
                    val body = action.value.takeIf { action.type == ActionType.CLICK_TEXT && it.isNotBlank() }
                        ?: if (action.type == ActionType.SHARE) context.str(R.string.confirm_share_body, action.value)
                        else context.str(R.string.confirm_action_body, ActionDescriber.describe(context, action))
                    val label = when {
                        action.type == ActionType.SHARE -> context.str(R.string.btn_share)
                        action.target.contains("send", true) || action.target.contains("भेज") -> context.str(R.string.btn_send)
                        else -> context.str(R.string.btn_continue)
                    }
                    if (!ui.confirm(UserPrompt.Confirm(context.str(R.string.confirm_title), body, label))) {
                        return StepResult.fail(context.str(R.string.msg_cancelled_user))
                    }
                }
                runWithRetry(action)
            }
        }
    }

    private suspend fun runWithRetry(action: Action): StepResult {
        val tries = if (action.type.needsAccessibility) settings.retryLimit.value + 1 else 1
        var last = StepResult.fail(context.str(R.string.msg_unsupported))
        for (attempt in 0 until tries) {
            val adapter = adapters.forPackage(AccessibilityStateManager.currentPackage.value)
            val r = withTimeoutOrNull(settings.timeoutSec.value * 1000L) { adapter.executeAction(action, env) }
                ?: StepResult.fail(context.str(R.string.msg_timeout))
            if (r.ok || !r.retryable) return r
            last = r
            if (attempt < tries - 1) delay(700)
        }
        return last
    }

    private suspend fun dial(action: Action, ui: UserInteraction): StepResult {
        val target = action.target.trim()
        val number: String
        val label: String
        if (ActionValidator.isPhoneNumber(target)) {
            number = target.filter { it.isDigit() || it == '+' }
            label = number
        } else {
            if (!contacts.hasPermission()) return StepResult.fail(context.str(R.string.msg_contacts_permission))
            val matches = contacts.find(target)
            if (matches.isEmpty()) return StepResult.fail(context.str(R.string.msg_contact_not_found, target))
            val chosen = if (matches.size == 1) matches[0] else {
                val idx = ui.choose(UserPrompt.Choice(context.str(R.string.choice_title), matches.map { "${it.name} — ${it.number}" }))
                    ?: return StepResult.fail(context.str(R.string.msg_cancelled_user))
                matches[idx]
            }
            number = chosen.number
            label = "${chosen.name} (${chosen.number})"
        }
        if (!ActionValidator.isPhoneNumber(number)) return StepResult.fail(context.str(R.string.msg_invalid_number))
        val ok = ui.confirm(UserPrompt.Confirm(context.str(R.string.confirm_title), context.str(R.string.confirm_call_body, label), context.str(R.string.btn_call)))
        if (!ok) return StepResult.fail(context.str(R.string.msg_cancelled_user))
        // ACTION_DIAL only opens the dialer; the user still presses the green call button.
        return if (launcher.dial(number)) StepResult.ok(label) else StepResult.fail(context.str(R.string.msg_unsupported))
    }

    private suspend fun nested(action: Action, ui: UserInteraction, depth: Int): StepResult {
        if (depth >= MAX_DEPTH) return StepResult.fail(context.str(R.string.msg_workflow_depth))
        val wf = workflows().firstOrNull { it.id == action.target }
            ?: return StepResult.fail(context.str(R.string.msg_workflow_not_found))
        if (!wf.enabled) return StepResult.fail(context.str(R.string.msg_workflow_disabled, wf.name))
        if (wf.requiresConfirmation || action.confirmationRequired) {
            val ok = ui.confirm(UserPrompt.Confirm(context.str(R.string.confirm_title), context.str(R.string.confirm_workflow_body, wf.name), context.str(R.string.btn_run)))
            if (!ok) return StepResult.fail(context.str(R.string.msg_cancelled_user))
        }
        val r = runPlan(ActionPlan(wf.name, wf.actions), ui, depth + 1)
        return StepResult(r.ok, r.message)
    }

    private fun addLog(desc: String, ok: Boolean, msg: String) {
        _log.value = _log.value + LogEntry(System.currentTimeMillis(), desc, ok, msg)
    }

    private companion object { const val MAX_DEPTH = 3 }
}
