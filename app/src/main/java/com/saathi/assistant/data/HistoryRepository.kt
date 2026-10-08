package com.saathi.assistant.data

import com.saathi.assistant.util.newId
import com.saathi.assistant.workflow.WorkflowJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

@Serializable
data class HistoryEntry(
    val id: String = newId(),
    val time: Long = System.currentTimeMillis(),
    val command: String,
    val success: Boolean,
    val message: String,
    val steps: List<String> = emptyList()
)

/** Automation history, stored only on this device (last 100 runs). */
class HistoryRepository(private val file: File) {
    private val serializer = ListSerializer(HistoryEntry.serializer())
    private val _items = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _items.asStateFlow()

    init {
        if (file.exists()) {
            _items.value = runCatching { WorkflowJson.json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
        }
    }

    @Synchronized fun add(e: HistoryEntry) {
        _items.value = (listOf(e) + _items.value).take(MAX)
        persist()
    }

    @Synchronized fun clear() { _items.value = emptyList(); persist() }

    private fun persist() {
        runCatching { file.writeText(WorkflowJson.json.encodeToString(serializer, _items.value)) }
    }

    private companion object { const val MAX = 100 }
}
