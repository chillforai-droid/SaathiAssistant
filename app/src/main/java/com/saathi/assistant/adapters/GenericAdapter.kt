package com.saathi.assistant.adapters

import android.accessibilityservice.AccessibilityService
import com.saathi.assistant.R
import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.automation.StepResult
import com.saathi.assistant.util.str

/** Handles all core actions for any app. */
open class GenericAdapter : AppAdapter {
    override val id: String = "generic"
    override val app: KnownApp? = null
    override fun canHandle(packageName: String) = true

    override fun supportedActions(): Set<ActionType> =
        ActionType.values().toSet() - setOf(ActionType.DIAL, ActionType.RUN_WORKFLOW)

    override suspend fun executeAction(action: Action, env: AdapterEnv): StepResult {
        val ctx = env.context
        return when (action.type) {
            ActionType.OPEN_APP -> {
                val r = env.launcher.resolve(action.target)
                    ?: return StepResult.fail(ctx.str(R.string.msg_app_not_installed, action.target))
                if (env.launcher.launch(r)) StepResult.ok(ctx.str(R.string.msg_opened, r.label))
                else StepResult.fail(ctx.str(R.string.msg_app_not_installed, r.label))
            }
            ActionType.OPEN_URI ->
                if (env.launcher.openUri(action.target)) StepResult.ok() else StepResult.fail(ctx.str(R.string.msg_uri_failed))
            ActionType.OPEN_SETTINGS ->
                if (env.launcher.openSettings(action.target)) StepResult.ok() else StepResult.fail(ctx.str(R.string.msg_settings_failed))
            ActionType.SHARE ->
                if (env.launcher.share(action.value)) StepResult.ok() else StepResult.fail(ctx.str(R.string.msg_share_failed))
            ActionType.WAIT -> StepResult.ok()
            ActionType.BACK -> env.executor.global(AccessibilityService.GLOBAL_ACTION_BACK)
            ActionType.HOME -> env.executor.global(AccessibilityService.GLOBAL_ACTION_HOME)
            ActionType.RECENTS -> env.executor.global(AccessibilityService.GLOBAL_ACTION_RECENTS)
            ActionType.SCREENSHOT -> env.executor.global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            ActionType.CLICK_TEXT -> env.executor.clickText(action.target)
            ActionType.CLICK_ID -> env.executor.clickId(action.target)
            ActionType.TYPE_TEXT -> env.executor.typeText(action.value, action.target)
            ActionType.SCROLL -> env.executor.scroll(action.target)
            ActionType.DIAL, ActionType.RUN_WORKFLOW -> StepResult.fail(ctx.str(R.string.msg_unsupported))
        }
    }
}
