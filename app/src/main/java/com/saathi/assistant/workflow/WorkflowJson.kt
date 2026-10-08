package com.saathi.assistant.workflow

import com.saathi.assistant.automation.ActionValidator
import com.saathi.assistant.safety.SafetyClassifier
import com.saathi.assistant.safety.SafetyLevel
import com.saathi.assistant.util.newId
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/** Export / import / validation of workflow JSON. Imported data is never trusted. */
object WorkflowJson {
    const val MAX_BYTES = 256 * 1024
    const val MAX_TRIGGERS = 20

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    enum class ImportError { TOO_LARGE, INVALID_JSON, EMPTY, NO_NAME, TOO_MANY_ACTIONS, INVALID_ACTION, BLOCKED_ACTION }

    sealed interface ImportResult {
        data class Success(val workflows: List<Workflow>) : ImportResult
        data class Failure(val error: ImportError, val detail: String = "") : ImportResult
    }

    fun export(w: Workflow): String = json.encodeToString(Workflow.serializer(), w)
    fun exportAll(list: List<Workflow>): String = json.encodeToString(ListSerializer(Workflow.serializer()), list)

    fun parse(text: String): ImportResult {
        if (text.toByteArray(Charsets.UTF_8).size > MAX_BYTES) return ImportResult.Failure(ImportError.TOO_LARGE)
        val parsed: List<Workflow> = try {
            val element = json.parseToJsonElement(text)
            if (element is JsonArray) json.decodeFromJsonElement(ListSerializer(Workflow.serializer()), element)
            else listOf(json.decodeFromJsonElement(Workflow.serializer(), element))
        } catch (e: Exception) {
            return ImportResult.Failure(ImportError.INVALID_JSON, e.message.orEmpty().take(120))
        }
        if (parsed.isEmpty()) return ImportResult.Failure(ImportError.EMPTY)

        val now = System.currentTimeMillis()
        val cleaned = ArrayList<Workflow>()
        for (w in parsed) {
            val name = w.name.trim().take(80)
            if (name.isEmpty()) return ImportResult.Failure(ImportError.NO_NAME)
            if (w.actions.size > ActionValidator.MAX_STEPS) return ImportResult.Failure(ImportError.TOO_MANY_ACTIONS)
            for ((i, a) in w.actions.withIndex()) {
                ActionValidator.validate(a)?.let { return ImportResult.Failure(ImportError.INVALID_ACTION, "$name, step ${i + 1}: $it") }
                if (SafetyClassifier.classify(a).level == SafetyLevel.BLOCK) {
                    return ImportResult.Failure(ImportError.BLOCKED_ACTION, "$name, step ${i + 1}")
                }
            }
            cleaned += w.copy(
                id = newId(),
                name = name,
                description = w.description.take(500),
                triggerPhrases = w.triggerPhrases.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(MAX_TRIGGERS),
                actions = w.actions.map { it.copy(id = newId()) },
                favorite = false,
                createdAt = now,
                updatedAt = now
            )
        }
        return ImportResult.Success(cleaned)
    }
}
