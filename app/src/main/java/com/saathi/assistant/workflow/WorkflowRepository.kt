package com.saathi.assistant.workflow

import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.util.newId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/** Local-only persistence (a JSON file in app-private storage). No server, no cloud. */
class WorkflowRepository(private val file: File) {
    private val serializer = ListSerializer(Workflow.serializer())
    private val _items = MutableStateFlow<List<Workflow>>(emptyList())
    val workflows: StateFlow<List<Workflow>> = _items.asStateFlow()

    init { load() }

    @Synchronized private fun load() {
        if (!file.exists()) {
            _items.value = listOf(sample())
            persist()
            return
        }
        _items.value = runCatching { WorkflowJson.json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
    }

    @Synchronized private fun persist() {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(WorkflowJson.json.encodeToString(serializer, _items.value))
            if (!tmp.renameTo(file)) { file.writeText(tmp.readText()); tmp.delete() }
        }
    }

    fun get(id: String): Workflow? = _items.value.firstOrNull { it.id == id }

    @Synchronized fun upsert(w: Workflow) {
        val updated = w.copy(updatedAt = System.currentTimeMillis())
        val list = _items.value
        _items.value = if (list.any { it.id == w.id }) list.map { if (it.id == w.id) updated else it } else list + updated
        persist()
    }

    @Synchronized fun addAll(ws: List<Workflow>) { _items.value = _items.value + ws; persist() }

    @Synchronized fun delete(id: String) { _items.value = _items.value.filterNot { it.id == id }; persist() }

    @Synchronized fun duplicate(id: String, copySuffix: String) {
        val w = get(id) ?: return
        val now = System.currentTimeMillis()
        upsert(w.copy(id = newId(), name = w.name + copySuffix, actions = w.actions.map { it.copy(id = newId()) }, favorite = false, createdAt = now, updatedAt = now))
    }

    fun setEnabled(id: String, enabled: Boolean) { get(id)?.let { upsert(it.copy(enabled = enabled)) } }
    fun toggleFavorite(id: String) { get(id)?.let { upsert(it.copy(favorite = !it.favorite)) } }

    private fun sample() = Workflow(
        name = "YouTube Upload Prep",
        description = "Opens YouTube Studio, then Files, then waits 1 second.",
        triggerPhrases = listOf("youtube upload prep", "upload prep"),
        actions = listOf(
            Action(type = ActionType.OPEN_APP, target = "youtube_studio"),
            Action(type = ActionType.OPEN_APP, target = "files", delayMs = 2000),
            Action(type = ActionType.WAIT, delayMs = 1000)
        )
    )
}
