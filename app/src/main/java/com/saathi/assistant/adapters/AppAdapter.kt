package com.saathi.assistant.adapters

import android.content.Context
import com.saathi.assistant.accessibility.AccessibilityActionExecutor
import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.automation.AppLauncher
import com.saathi.assistant.automation.StepResult
import com.saathi.assistant.command.ActionPlan

/** Everything an adapter needs to act. */
class AdapterEnv(val context: Context, val launcher: AppLauncher, val executor: AccessibilityActionExecutor)

/** Lets the Android layer supply the localized confirmation text shown before a message is sent. */
fun interface SendConfirmText { fun text(contact: String, appName: String, message: String): String }

/**
 * Per-app behaviour. The generic adapter handles every core action; app adapters add
 * app-specific plans (e.g. the guided "send message" flow) and metadata, so the engine never
 * turns into one giant if/else.
 */
interface AppAdapter {
    val id: String
    val app: KnownApp?
    fun canHandle(packageName: String): Boolean
    fun supportedActions(): Set<ActionType>
    suspend fun executeAction(action: Action, env: AdapterEnv): StepResult
    /** Guided "send a message to a contact" plan, or null when this app has none. */
    fun buildSendMessagePlan(contact: String, message: String, confirm: SendConfirmText): ActionPlan? = null
}
