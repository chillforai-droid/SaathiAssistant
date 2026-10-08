package com.saathi.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * The service itself is intentionally tiny: it keeps NO copy of screen content and sends nothing
 * anywhere. It only exposes the live window to [AccessibilityActionExecutor] while the user has
 * it enabled, and only when an automation the user started asks for it.
 */
class SaathiAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityStateManager.onConnected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                e.packageName?.toString()?.let { AccessibilityStateManager.onPackageChanged(it) }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        AccessibilityStateManager.onDisconnected(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AccessibilityStateManager.onDisconnected(this)
        super.onDestroy()
    }
}
