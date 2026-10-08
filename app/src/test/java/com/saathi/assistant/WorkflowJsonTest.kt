package com.saathi.assistant

import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.workflow.Workflow
import com.saathi.assistant.workflow.WorkflowJson
import com.saathi.assistant.workflow.WorkflowJson.ImportError
import com.saathi.assistant.workflow.WorkflowJson.ImportResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowJsonTest {
    private val sample = Workflow(
        name = "YouTube Upload Prep",
        description = "d",
        triggerPhrases = listOf("upload prep"),
        actions = listOf(
            Action(type = ActionType.OPEN_APP, target = "youtube_studio"),
            Action(type = ActionType.WAIT, delayMs = 1000)
        )
    )

    @Test fun roundTrip() {
        val r = WorkflowJson.parse(WorkflowJson.export(sample)) as ImportResult.Success
        val w = r.workflows.single()
        assertEquals(sample.name, w.name)
        assertEquals(sample.actions.map { it.type to it.target to it.delayMs }, w.actions.map { it.type to it.target to it.delayMs })
        assertNotEquals("ids are regenerated on import", sample.id, w.id)
    }

    @Test fun acceptsArray() {
        val r = WorkflowJson.parse(WorkflowJson.exportAll(listOf(sample, sample.copy(name = "B")))) as ImportResult.Success
        assertEquals(2, r.workflows.size)
    }

    @Test fun rejectsGarbage() {
        assertEquals(ImportError.INVALID_JSON, (WorkflowJson.parse("not json") as ImportResult.Failure).error)
        assertEquals(ImportError.INVALID_JSON, (WorkflowJson.parse("{\"name\":\"x\",\"actions\":[{\"type\":\"HACK\"}]}") as ImportResult.Failure).error)
    }

    @Test fun rejectsInvalidAndBlockedSteps() {
        val bad = sample.copy(actions = listOf(Action(type = ActionType.OPEN_APP)))
        assertEquals(ImportError.INVALID_ACTION, (WorkflowJson.parse(WorkflowJson.export(bad)) as ImportResult.Failure).error)
        val blocked = sample.copy(actions = listOf(Action(type = ActionType.TYPE_TEXT, value = "123456")))
        assertEquals(ImportError.BLOCKED_ACTION, (WorkflowJson.parse(WorkflowJson.export(blocked)) as ImportResult.Failure).error)
        val noName = sample.copy(name = "  ")
        assertEquals(ImportError.NO_NAME, (WorkflowJson.parse(WorkflowJson.export(noName)) as ImportResult.Failure).error)
    }

    @Test fun rejectsOversizedInput() {
        val huge = "x".repeat(WorkflowJson.MAX_BYTES + 1)
        assertTrue(WorkflowJson.parse(huge) is ImportResult.Failure)
    }
}
