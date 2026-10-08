package com.saathi.assistant

import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.safety.SafetyClassifier
import com.saathi.assistant.safety.SafetyLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class SafetyClassifierTest {
    private fun level(a: Action) = SafetyClassifier.classify(a).level

    @Test fun safeActions() {
        assertEquals(SafetyLevel.SAFE, level(Action(type = ActionType.OPEN_APP, target = "whatsapp")))
        assertEquals(SafetyLevel.SAFE, level(Action(type = ActionType.OPEN_SETTINGS, target = "wifi")))
        assertEquals(SafetyLevel.SAFE, level(Action(type = ActionType.SCROLL, target = "down")))
        assertEquals(SafetyLevel.SAFE, level(Action(type = ActionType.TYPE_TEXT, value = "Hello")))
    }

    @Test fun confirmActions() {
        assertEquals(SafetyLevel.CONFIRM, level(Action(type = ActionType.DIAL, target = "Rahul")))
        assertEquals(SafetyLevel.CONFIRM, level(Action(type = ActionType.SHARE, value = "hi")))
        assertEquals(SafetyLevel.CONFIRM, level(Action(type = ActionType.CLICK_TEXT, target = "Send|भेजें")))
        assertEquals(SafetyLevel.CONFIRM, level(Action(type = ActionType.CLICK_TEXT, target = "Delete")))
        assertEquals(SafetyLevel.CONFIRM, level(Action(type = ActionType.OPEN_APP, target = "files", confirmationRequired = true)))
        assertEquals(SafetyLevel.CONFIRM, level(Action(type = ActionType.OPEN_URI, target = "tel:100")))
    }

    @Test fun blockedActions() {
        assertEquals(SafetyLevel.BLOCK, level(Action(type = ActionType.TYPE_TEXT, value = "123456")))
        assertEquals(SafetyLevel.BLOCK, level(Action(type = ActionType.TYPE_TEXT, value = "my password is x")))
        assertEquals(SafetyLevel.BLOCK, level(Action(type = ActionType.TYPE_TEXT, target = "OTP", value = "abc")))
        assertEquals(SafetyLevel.BLOCK, level(Action(type = ActionType.CLICK_TEXT, target = "Pay now")))
        assertEquals(SafetyLevel.BLOCK, level(Action(type = ActionType.CLICK_TEXT, target = "Confirm UPI PIN")))
        assertEquals(SafetyLevel.BLOCK, level(Action(type = ActionType.CLICK_TEXT, target = "Factory reset")))
        assertEquals(SafetyLevel.BLOCK, level(Action(type = ActionType.CLICK_TEXT, target = "पासवर्ड")))
    }
}
