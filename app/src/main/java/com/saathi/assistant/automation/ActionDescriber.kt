package com.saathi.assistant.automation

import android.content.Context
import androidx.annotation.StringRes
import com.saathi.assistant.R
import com.saathi.assistant.safety.SafetyReason

@StringRes
fun ActionType.labelRes(): Int = when (this) {
    ActionType.OPEN_APP -> R.string.act_open_app
    ActionType.OPEN_URI -> R.string.act_open_uri
    ActionType.OPEN_SETTINGS -> R.string.act_open_settings
    ActionType.DIAL -> R.string.act_dial
    ActionType.BACK -> R.string.act_back
    ActionType.HOME -> R.string.act_home
    ActionType.RECENTS -> R.string.act_recents
    ActionType.SCROLL -> R.string.act_scroll
    ActionType.CLICK_TEXT -> R.string.act_click_text
    ActionType.CLICK_ID -> R.string.act_click_id
    ActionType.TYPE_TEXT -> R.string.act_type_text
    ActionType.WAIT -> R.string.act_wait
    ActionType.SCREENSHOT -> R.string.act_screenshot
    ActionType.SHARE -> R.string.act_share
    ActionType.RUN_WORKFLOW -> R.string.act_run_workflow
}

@StringRes
fun ActionType.hintRes(): Int = when (this) {
    ActionType.OPEN_APP -> R.string.hint_open_app
    ActionType.OPEN_URI -> R.string.hint_open_uri
    ActionType.OPEN_SETTINGS -> R.string.hint_open_settings
    ActionType.DIAL -> R.string.hint_dial
    ActionType.SCROLL -> R.string.hint_scroll
    ActionType.CLICK_TEXT -> R.string.hint_click_text
    ActionType.CLICK_ID -> R.string.hint_click_id
    ActionType.TYPE_TEXT -> R.string.hint_type_text
    ActionType.WAIT -> R.string.hint_wait
    ActionType.SHARE -> R.string.hint_share
    ActionType.RUN_WORKFLOW -> R.string.hint_run_workflow
    ActionType.BACK, ActionType.HOME, ActionType.RECENTS, ActionType.SCREENSHOT -> R.string.hint_none
}

@StringRes
fun SafetyReason.resId(): Int = when (this) {
    SafetyReason.NONE -> R.string.reason_none
    SafetyReason.CREDENTIAL -> R.string.reason_credential
    SafetyReason.FINANCIAL -> R.string.reason_financial
    SafetyReason.SECURITY_CONTROL -> R.string.reason_security
    SafetyReason.SENSITIVE_ACTION -> R.string.reason_sensitive
    SafetyReason.CALL -> R.string.reason_call
    SafetyReason.SHARE -> R.string.reason_share
    SafetyReason.USER_MARKED -> R.string.reason_user_marked
    SafetyReason.PHONE_URI -> R.string.reason_call
}

object ActionDescriber {
    fun describe(context: Context, a: Action): String {
        val label = context.getString(a.type.labelRes())
        val detail = when (a.type) {
            ActionType.TYPE_TEXT, ActionType.SHARE -> a.value
            ActionType.WAIT -> "${a.delayMs} ms"
            else -> a.target
        }.take(80)
        return if (detail.isBlank()) label else "$label: $detail"
    }
}
