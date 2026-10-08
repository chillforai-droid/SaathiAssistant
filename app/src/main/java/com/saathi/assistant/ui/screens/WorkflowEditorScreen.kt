package com.saathi.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.saathi.assistant.R
import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionDescriber
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.automation.ActionValidator
import com.saathi.assistant.automation.hintRes
import com.saathi.assistant.automation.labelRes
import com.saathi.assistant.safety.SafetyClassifier
import com.saathi.assistant.safety.SafetyLevel
import com.saathi.assistant.ui.MainViewModel
import com.saathi.assistant.ui.ScreenColumn
import com.saathi.assistant.ui.SectionTitle

@Composable
fun WorkflowEditorScreen(vm: MainViewModel, nav: NavController) {
    val context = LocalContext.current
    val draft by vm.draft.collectAsStateWithLifecycle()
    val workflows by vm.workflows.collectAsStateWithLifecycle()
    var triggers by remember { mutableStateOf(draft.triggerPhrases.joinToString(", ")) }
    var editing by remember { mutableStateOf<Action?>(null) }
    var adding by remember { mutableStateOf(false) }

    ScreenColumn {
        OutlinedTextField(
            value = draft.name, onValueChange = { v -> vm.updateDraft { it.copy(name = v.take(80)) } },
            label = { Text(stringResource(R.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.description, onValueChange = { v -> vm.updateDraft { it.copy(description = v.take(500)) } },
            label = { Text(stringResource(R.string.field_description)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = triggers,
            onValueChange = { v ->
                triggers = v
                vm.updateDraft { it.copy(triggerPhrases = v.split(",", "\n").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }.take(20)) }
            },
            label = { Text(stringResource(R.string.field_triggers)) }, modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = draft.requiresConfirmation, onCheckedChange = { v -> vm.updateDraft { it.copy(requiresConfirmation = v) } })
            Text(stringResource(R.string.field_requires_confirmation), Modifier.padding(start = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = draft.enabled, onCheckedChange = { v -> vm.updateDraft { it.copy(enabled = v) } })
            Text(stringResource(R.string.field_enabled), Modifier.padding(start = 8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { SectionTitle(stringResource(R.string.field_actions, draft.actions.size)) }
            OutlinedButton(onClick = { adding = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = null)
                Text(stringResource(R.string.btn_add_action), Modifier.padding(start = 6.dp))
            }
        }
        draft.actions.forEachIndexed { i, a ->
            val level = SafetyClassifier.classify(a).level
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${i + 1}. ${ActionDescriber.describe(context, a)}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    val meta = buildString {
                        if (a.delayMs > 0 && a.type != ActionType.WAIT) append(stringResource(R.string.delay_before, a.delayMs.toInt())).append("  ")
                        if (level == SafetyLevel.CONFIRM) append(stringResource(R.string.level_confirm))
                        if (level == SafetyLevel.BLOCK) append(stringResource(R.string.level_block))
                    }
                    if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = if (level == SafetyLevel.BLOCK) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = a.enabled, onCheckedChange = { v -> vm.upsertDraftAction(a.copy(enabled = v)) })
                        IconButton(onClick = { vm.moveDraftAction(a.id, -1) }, enabled = i > 0) { Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.cd_move_up)) }
                        IconButton(onClick = { vm.moveDraftAction(a.id, 1) }, enabled = i < draft.actions.size - 1) { Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.cd_move_down)) }
                        IconButton(onClick = { vm.duplicateDraftAction(a.id) }) { Icon(Icons.Default.AddCircle, stringResource(R.string.btn_duplicate)) }
                        IconButton(onClick = { editing = a }) { Icon(Icons.Default.Edit, stringResource(R.string.btn_edit)) }
                        IconButton(onClick = { vm.deleteDraftAction(a.id) }) { Icon(Icons.Default.Delete, stringResource(R.string.btn_delete)) }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::testDraft) { Text(stringResource(R.string.btn_test)) }
            Button(onClick = { if (vm.saveDraft()) nav.popBackStack() }) { Text(stringResource(R.string.btn_save)) }
        }
    }

    if (adding || editing != null) {
        ActionDialog(
            initial = editing,
            otherWorkflows = workflows.filter { it.id != draft.id },
            onDismiss = { adding = false; editing = null },
            onSave = { vm.upsertDraftAction(it); adding = false; editing = null }
        )
    }
}

@Composable
private fun ActionDialog(initial: Action?, otherWorkflows: List<com.saathi.assistant.workflow.Workflow>, onDismiss: () -> Unit, onSave: (Action) -> Unit) {
    var type by remember { mutableStateOf(initial?.type ?: ActionType.OPEN_APP) }
    var target by remember { mutableStateOf(initial?.target ?: "") }
    var value by remember { mutableStateOf(initial?.value ?: "") }
    var delay by remember { mutableStateOf((initial?.delayMs ?: 0L).toString()) }
    var confirm by remember { mutableStateOf(initial?.confirmationRequired ?: false) }
    var menu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.btn_add_action else R.string.btn_edit)) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    OutlinedButton(onClick = { menu = true }) { Text(stringResource(type.labelRes())) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        ActionType.values().forEach { t ->
                            DropdownMenuItem(text = { Text(stringResource(t.labelRes())) }, onClick = { type = t; menu = false })
                        }
                    }
                }
                Text(stringResource(type.hintRes()), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text(stringResource(R.string.field_target)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (type == ActionType.RUN_WORKFLOW) otherWorkflows.forEach { w -> TextButton(onClick = { target = w.id }) { Text(w.name) } }
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(stringResource(R.string.field_value)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = delay, onValueChange = { delay = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.field_delay)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = confirm, onCheckedChange = { confirm = it })
                    Text(stringResource(R.string.field_confirm_step), Modifier.padding(start = 8.dp))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = Action(
                    id = initial?.id ?: com.saathi.assistant.util.newId(), type = type, target = target.trim(), value = value,
                    delayMs = delay.toLongOrNull() ?: 0L, confirmationRequired = confirm, enabled = initial?.enabled ?: true
                )
                val problem = ActionValidator.validate(a)
                    ?: if (SafetyClassifier.classify(a).level == SafetyLevel.BLOCK) "Blocked for safety" else null
                if (problem == null) onSave(a) else error = problem
            }) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}
