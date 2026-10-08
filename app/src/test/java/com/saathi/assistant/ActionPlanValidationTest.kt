package com.saathi.assistant

import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.automation.ActionValidator
import com.saathi.assistant.command.ActionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionPlanValidationTest {
    @Test fun validPlanHasNoIssues() {
        val plan = ActionPlan("t", listOf(
            Action(type = ActionType.OPEN_APP, target = "whatsapp"),
            Action(type = ActionType.WAIT, delayMs = 1000),
            Action(type = ActionType.SCROLL, target = "down")
        ))
        assertTrue(plan.validate().isEmpty())
    }

    @Test fun emptyPlanIsInvalid() = assertFalse(ActionPlan("t", emptyList()).validate().isEmpty())

    @Test fun rejectsBadActions() {
        assertNotNull(ActionValidator.validate(Action(type = ActionType.OPEN_APP)))
        assertNotNull(ActionValidator.validate(Action(type = ActionType.WAIT, delayMs = 0)))
        assertNotNull(ActionValidator.validate(Action(type = ActionType.SCROLL, target = "sideways")))
        assertNotNull(ActionValidator.validate(Action(type = ActionType.OPEN_SETTINGS, target = "nope")))
        assertNotNull(ActionValidator.validate(Action(type = ActionType.OPEN_URI, target = "javascript:alert(1)")))
        assertNotNull(ActionValidator.validate(Action(type = ActionType.OPEN_URI, target = "intent://x#Intent;end")))
        assertNotNull(ActionValidator.validate(Action(type = ActionType.CLICK_TEXT, delayMs = 999_999, target = "x")))
    }

    @Test fun acceptsGoodActions() {
        assertNull(ActionValidator.validate(Action(type = ActionType.OPEN_URI, target = "https://example.com")))
        assertNull(ActionValidator.validate(Action(type = ActionType.OPEN_SETTINGS)))
        assertNull(ActionValidator.validate(Action(type = ActionType.BACK)))
    }

    @Test fun phoneNumbers() {
        assertTrue(ActionValidator.isPhoneNumber("+91 98765-43210"))
        assertFalse(ActionValidator.isPhoneNumber("*#06#"))
        assertFalse(ActionValidator.isPhoneNumber("12"))
        assertFalse(ActionValidator.isPhoneNumber("98+765"))
    }
}
