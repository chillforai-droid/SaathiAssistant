package com.saathi.assistant.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saathi.assistant.R
import com.saathi.assistant.SaathiApplication
import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionEngine
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.automation.ActionValidator
import com.saathi.assistant.automation.UserInteraction
import com.saathi.assistant.automation.UserPrompt
import com.saathi.assistant.command.ActionPlan
import com.saathi.assistant.command.Command
import com.saathi.assistant.command.CommandSource
import com.saathi.assistant.command.InterpretResult
import com.saathi.assistant.command.ParseError
import com.saathi.assistant.data.HistoryEntry
import com.saathi.assistant.permissions.PermissionHelper
import com.saathi.assistant.permissions.SystemStatus
import com.saathi.assistant.safety.SafetyClassifier
import com.saathi.assistant.safety.SafetyLevel
import com.saathi.assistant.util.Notifier
import com.saathi.assistant.util.newId
import com.saathi.assistant.util.str
import com.saathi.assistant.workflow.Workflow
import com.saathi.assistant.workflow.WorkflowJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class PendingPrompt(val prompt: UserPrompt, val deferred: CompletableDeferred<Any?>)

class MainViewModel(private val app: Application) : AndroidViewModel(app), UserInteraction {
    val container = (app as SaathiApplication).container
    private val engine = container.engine

    // ---- state exposed to the UI ----
    val engineState: StateFlow<ActionEngine.State> = engine.state
    val workflows = container.workflows.workflows
    val history = container.history.entries
    val settings = container.settings

    val recent: StateFlow<List<String>> = history
        .map { list -> list.map { it.command }.distinct().take(6) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _commandText = MutableStateFlow("")
    val commandText: StateFlow<String> = _commandText.asStateFlow()
    fun setCommand(v: String) { _commandText.value = v }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private fun say(text: String) { _messages.tryEmit(text) }

    private val _status = MutableStateFlow(SystemStatus())
    val status: StateFlow<SystemStatus> = _status.asStateFlow()
    fun refreshStatus() { _status.value = PermissionHelper.status(app) }

    // ---- confirmation / choice dialogs ----
    private val _prompt = MutableStateFlow<PendingPrompt?>(null)
    val prompt: StateFlow<PendingPrompt?> = _prompt.asStateFlow()

    override suspend fun confirm(prompt: UserPrompt.Confirm): Boolean {
        val d = CompletableDeferred<Any?>()
        _prompt.value = PendingPrompt(prompt, d)
        try { return d.await() == true } finally { _prompt.value = null }
    }

    override suspend fun choose(prompt: UserPrompt.Choice): Int? {
        val d = CompletableDeferred<Any?>()
        _prompt.value = PendingPrompt(prompt, d)
        try { return d.await() as? Int } finally { _prompt.value = null }
    }

    fun answerPrompt(value: Any?) { _prompt.value?.deferred?.complete(value) }

    // ---- voice ----
    private val speech = SpeechController(app) { text -> submit(text, CommandSource.VOICE) }
    val voice: StateFlow<VoiceState> = speech.state
    val partial: StateFlow<String> = speech.partial
    fun startListening() = speech.start(settings.speechLang.value)
    fun stopListening() = speech.stop()
    fun clearVoiceError() = speech.clearError()
    override fun onCleared() { speech.stop(); super.onCleared() }

    // ---- running commands ----
    private var job: Job? = null
    val isBusy: Boolean get() = job?.isActive == true

    fun errorText(e: ParseError): String = app.str(
        when (e) {
            ParseError.EMPTY -> R.string.err_empty
            ParseError.NO_CONTACT -> R.string.err_no_contact
            ParseError.NO_MESSAGE -> R.string.err_no_message
            ParseError.NO_APP -> R.string.err_no_app
            ParseError.NO_TARGET -> R.string.err_no_target
            ParseError.NO_TEXT -> R.string.err_no_text
            ParseError.UNRECOGNIZED -> R.string.err_unrecognized
            ParseError.APP_UNSUPPORTED -> R.string.err_app_unsupported
        }
    )

    /** Parse without executing (Commands screen preview). */
    fun preview(text: String): InterpretResult = container.interpreter.interpret(Command(text))

    fun submit(text: String, source: CommandSource = CommandSource.TEXT) {
        val t = text.trim()
        if (t.isEmpty()) return
        if (isBusy) { say(app.str(R.string.msg_busy)); return }
        when (val r = container.interpreter.interpret(Command(t, source))) {
            is InterpretResult.Plan -> { _commandText.value = ""; start(r.plan, t) }
            is InterpretResult.Failure -> {
                val msg = errorText(r.error)
                container.history.add(HistoryEntry(command = t, success = false, message = msg))
                say(msg)
            }
        }
    }

    fun quickOpen(appKey: String) {
        val step = if (appKey == "settings") Action(type = ActionType.OPEN_SETTINGS) else Action(type = ActionType.OPEN_APP, target = appKey)
        if (isBusy) { say(app.str(R.string.msg_busy)); return }
        start(ActionPlan(appKey, listOf(step)), appKey)
    }

    fun runWorkflow(id: String) {
        val w = container.workflows.get(id) ?: return
        if (isBusy) { say(app.str(R.string.msg_busy)); return }
        start(ActionPlan(w.name, listOf(Action(type = ActionType.RUN_WORKFLOW, target = id))), w.name)
    }

    private fun start(plan: ActionPlan, label: String) {
        job = viewModelScope.launch {
            try {
                val r = engine.run(plan, this@MainViewModel)
                container.history.add(
                    HistoryEntry(command = label, success = r.ok, message = r.message, steps = engine.log.value.map { (if (it.ok) "✓ " else "✗ ") + it.description + if (it.message.isNotBlank()) " — ${it.message}" else "" })
                )
                say(r.message)
                if (plan.steps.size > 1 || plan.steps.firstOrNull()?.type == ActionType.RUN_WORKFLOW) {
                    Notifier.notifyRun(app, label, r.message)
                }
            } catch (e: CancellationException) {
                container.history.add(HistoryEntry(command = label, success = false, message = app.str(R.string.msg_run_cancelled), steps = engine.log.value.map { (if (it.ok) "✓ " else "✗ ") + it.description }))
                say(app.str(R.string.msg_run_cancelled))
                throw e
            }
        }
    }

    fun cancel() { job?.cancel() }

    // ---- workflow management ----
    fun deleteWorkflow(id: String) = container.workflows.delete(id)
    fun duplicateWorkflow(id: String) = container.workflows.duplicate(id, app.str(R.string.copy_suffix))
    fun toggleFavorite(id: String) = container.workflows.toggleFavorite(id)
    fun setWorkflowEnabled(id: String, v: Boolean) = container.workflows.setEnabled(id, v)
    fun clearHistory() = container.history.clear()

    fun exportJson(w: Workflow): String = WorkflowJson.export(w)

    fun importJson(text: String) {
        when (val r = WorkflowJson.parse(text)) {
            is WorkflowJson.ImportResult.Success -> {
                container.workflows.addAll(r.workflows)
                say(app.str(R.string.msg_imported, r.workflows.size))
            }
            is WorkflowJson.ImportResult.Failure -> say(
                app.str(
                    when (r.error) {
                        WorkflowJson.ImportError.TOO_LARGE -> R.string.imp_too_large
                        WorkflowJson.ImportError.INVALID_JSON -> R.string.imp_invalid_json
                        WorkflowJson.ImportError.EMPTY -> R.string.imp_empty
                        WorkflowJson.ImportError.NO_NAME -> R.string.imp_no_name
                        WorkflowJson.ImportError.TOO_MANY_ACTIONS -> R.string.imp_too_many
                        WorkflowJson.ImportError.INVALID_ACTION -> R.string.imp_invalid_action
                        WorkflowJson.ImportError.BLOCKED_ACTION -> R.string.imp_blocked_action
                    }
                ) + if (r.detail.isNotBlank()) " (${r.detail})" else ""
            )
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { app.contentResolver.openInputStream(uri)?.use { readLimited(it, WorkflowJson.MAX_BYTES) } }.getOrNull()
            }
            if (text == null) say(app.str(R.string.imp_too_large)) else importJson(text)
        }
    }

    private fun readLimited(input: InputStream, max: Int): String? {
        val buf = ByteArray(max + 1)
        var n = 0
        while (n < buf.size) {
            val r = input.read(buf, n, buf.size - n)
            if (r < 0) break
            n += r
        }
        return if (n > max) null else String(buf, 0, n, Charsets.UTF_8)
    }

    // ---- workflow editor draft (lives in the ViewModel so it survives rotation) ----
    private val _draft = MutableStateFlow(Workflow(name = ""))
    val draft: StateFlow<Workflow> = _draft.asStateFlow()
    private val _draftIsNew = MutableStateFlow(true)
    val draftIsNew: StateFlow<Boolean> = _draftIsNew.asStateFlow()

    fun openEditor(id: String?) {
        val existing = id?.let { container.workflows.get(it) }
        _draft.value = existing ?: Workflow(name = "")
        _draftIsNew.value = existing == null
    }

    fun updateDraft(block: (Workflow) -> Workflow) { _draft.value = block(_draft.value) }

    fun upsertDraftAction(a: Action) = updateDraft { w ->
        if (w.actions.any { it.id == a.id }) w.copy(actions = w.actions.map { if (it.id == a.id) a else it })
        else w.copy(actions = w.actions + a)
    }

    fun deleteDraftAction(id: String) = updateDraft { it.copy(actions = it.actions.filterNot { a -> a.id == id }) }

    fun duplicateDraftAction(id: String) = updateDraft { w ->
        val i = w.actions.indexOfFirst { it.id == id }
        if (i < 0) w else w.copy(actions = w.actions.toMutableList().apply { add(i + 1, w.actions[i].copy(id = newId())) })
    }

    fun moveDraftAction(id: String, delta: Int) = updateDraft { w ->
        val i = w.actions.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in w.actions.indices) w
        else w.copy(actions = w.actions.toMutableList().apply { val x = removeAt(i); add(j, x) })
    }

    /** Validates and saves; returns true on success. */
    fun saveDraft(): Boolean {
        val w = _draft.value
        if (w.name.isBlank()) { say(app.str(R.string.msg_name_required)); return false }
        w.actions.forEachIndexed { i, a ->
            ActionValidator.validate(a)?.let { say(app.str(R.string.msg_invalid_plan, "${i + 1}: $it")); return false }
            if (SafetyClassifier.classify(a).level == SafetyLevel.BLOCK) { say(app.str(R.string.imp_blocked_action) + " (${i + 1})"); return false }
        }
        container.workflows.upsert(w.copy(name = w.name.trim()))
        say(app.str(R.string.msg_saved))
        return true
    }

    /** Runs the draft's current steps without saving (the "Test workflow" button). */
    fun testDraft() {
        val w = _draft.value
        if (isBusy) { say(app.str(R.string.msg_busy)); return }
        if (w.actions.none { it.enabled }) { say(app.str(R.string.msg_no_steps)); return }
        start(ActionPlan(w.name.ifBlank { app.str(R.string.workflow_untitled) }, w.actions), w.name.ifBlank { app.str(R.string.workflow_untitled) })
    }
}
