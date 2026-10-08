package com.saathi.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.saathi.assistant.R
import com.saathi.assistant.automation.StepResult
import com.saathi.assistant.util.str

/**
 * Performs UI actions through the user-enabled accessibility service.
 * It refuses password fields and never reads or stores screen content beyond the single call.
 */
class AccessibilityActionExecutor(private val context: Context) {

    private fun service(): AccessibilityService? = AccessibilityStateManager.service
    private val notEnabled get() = StepResult.fail(context.str(R.string.msg_accessibility_off))

    fun global(action: Int): StepResult {
        val svc = service() ?: return notEnabled
        if (action == AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT && Build.VERSION.SDK_INT < 28) {
            return StepResult.fail(context.str(R.string.msg_unsupported))
        }
        return if (svc.performGlobalAction(action)) StepResult.ok() else StepResult.fail(context.str(R.string.msg_global_failed))
    }

    fun clickText(target: String): StepResult {
        val svc = service() ?: return notEnabled
        val root = svc.rootInActiveWindow ?: return StepResult.fail(context.str(R.string.msg_no_screen), retryable = true)
        val alternatives = target.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        var ambiguous = false
        for (alt in alternatives) {
            when (val m = AccessibilityNodeFinder.findBest(root, alt)) {
                is NodeMatch.Found -> return click(m.node, alt)
                NodeMatch.Ambiguous -> ambiguous = true
                NodeMatch.NotFound -> Unit
            }
        }
        return if (ambiguous) StepResult.fail(context.str(R.string.msg_ambiguous, target))
        else StepResult.fail(context.str(R.string.msg_not_found_on_screen, target), retryable = true)
    }

    fun clickId(viewId: String): StepResult {
        val svc = service() ?: return notEnabled
        val root = svc.rootInActiveWindow ?: return StepResult.fail(context.str(R.string.msg_no_screen), retryable = true)
        val node = AccessibilityNodeFinder.findById(root, viewId.trim())
            ?: return StepResult.fail(context.str(R.string.msg_not_found_on_screen, viewId), retryable = true)
        return click(node, viewId)
    }

    private fun click(node: AccessibilityNodeInfo, label: String): StepResult {
        val clickable = AccessibilityNodeFinder.clickableAncestor(node)
            ?: return StepResult.fail(context.str(R.string.msg_not_found_on_screen, label), retryable = true)
        return if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) StepResult.ok(label)
        else StepResult.fail(context.str(R.string.msg_global_failed), retryable = true)
    }

    fun typeText(text: String, fieldHint: String): StepResult {
        val svc = service() ?: return notEnabled
        val root = svc.rootInActiveWindow ?: return StepResult.fail(context.str(R.string.msg_no_screen), retryable = true)
        if (root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.isPassword == true) {
            return StepResult.fail(context.str(R.string.msg_password_field))
        }
        val field = AccessibilityNodeFinder.findEditable(root, fieldHint)
            ?: return StepResult.fail(context.str(R.string.msg_no_editable), retryable = true)
        if (field.isPassword) return StepResult.fail(context.str(R.string.msg_password_field))
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return if (field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) StepResult.ok()
        else StepResult.fail(context.str(R.string.msg_no_editable), retryable = true)
    }

    fun scroll(direction: String): StepResult {
        val svc = service() ?: return notEnabled
        val root = svc.rootInActiveWindow ?: return StepResult.fail(context.str(R.string.msg_no_screen), retryable = true)
        val node = AccessibilityNodeFinder.findScrollable(root) ?: return StepResult.fail(context.str(R.string.msg_no_scrollable))
        val action = when (direction.lowercase()) {
            "up" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP
            "left" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT
            "right" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT
            else -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN
        }
        val ok = node.performAction(action.id) ||
            node.performAction(
                if (direction.lowercase() in setOf("up", "left")) AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            )
        return if (ok) StepResult.ok() else StepResult.fail(context.str(R.string.msg_no_scrollable))
    }

    /** Visible, non-password text on screen (used only by the on-device "Inspect" demo). */
    fun visibleTexts(limit: Int = 40): List<String> {
        val root = service()?.rootInActiveWindow ?: return emptyList()
        return AccessibilityNodeFinder.allNodes(root)
            .filter { it.isVisibleToUser && !it.isPassword }
            .mapNotNull { it.text?.toString() ?: it.contentDescription?.toString() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(limit)
    }
}
