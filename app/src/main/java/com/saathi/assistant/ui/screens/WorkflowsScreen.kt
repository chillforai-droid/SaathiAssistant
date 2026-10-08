package com.saathi.assistant.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.saathi.assistant.R
import com.saathi.assistant.ui.MainViewModel
import com.saathi.assistant.ui.Routes
import com.saathi.assistant.ui.ScreenColumn
import com.saathi.assistant.ui.shareWorkflowFile
import com.saathi.assistant.workflow.Workflow

@Composable
fun WorkflowsScreen(vm: MainViewModel, nav: NavController) {
    val context = LocalContext.current
    val list by vm.workflows.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Workflow?>(null) }
    var showPaste by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importFromUri) }

    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.openEditor(null); nav.navigate(Routes.editor(null)) }) { Text(stringResource(R.string.btn_new_workflow)) }
            OutlinedButton(onClick = { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text(stringResource(R.string.btn_import_file)) }
            TextButton(onClick = { showPaste = true }) { Text(stringResource(R.string.btn_paste)) }
        }
        if (list.isEmpty()) Text(stringResource(R.string.workflows_empty))
        list.forEach { w ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(w.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { vm.toggleFavorite(w.id) }) {
                            Icon(if (w.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = stringResource(R.string.cd_favorite))
                        }
                    }
                    if (w.description.isNotBlank()) Text(w.description, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.workflow_meta, w.actions.size, w.triggerPhrases.joinToString(", ").ifBlank { "—" }),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = w.enabled, onCheckedChange = { vm.setWorkflowEnabled(w.id, it) })
                        Button(onClick = { vm.runWorkflow(w.id) }, enabled = w.enabled, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(R.string.btn_run)) }
                        IconButton(onClick = { vm.openEditor(w.id); nav.navigate(Routes.editor(w.id)) }) { Icon(Icons.Default.Edit, stringResource(R.string.btn_edit)) }
                        IconButton(onClick = { shareWorkflowFile(context, w) }) { Icon(Icons.Default.Share, stringResource(R.string.btn_export)) }
                        IconButton(onClick = { pendingDelete = w }) { Icon(Icons.Default.Delete, stringResource(R.string.btn_delete)) }
                    }
                }
            }
        }
    }

    pendingDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_workflow_title)) },
            text = { Text(w.name) },
            confirmButton = { Button(onClick = { vm.deleteWorkflow(w.id); pendingDelete = null }) { Text(stringResource(R.string.btn_delete)) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
    if (showPaste) {
        AlertDialog(
            onDismissRequest = { showPaste = false },
            title = { Text(stringResource(R.string.btn_paste)) },
            text = { OutlinedTextField(value = pasted, onValueChange = { pasted = it }, modifier = Modifier.fillMaxWidth(), minLines = 5, maxLines = 10) },
            confirmButton = { Button(onClick = { vm.importJson(pasted); pasted = ""; showPaste = false }) { Text(stringResource(R.string.btn_import)) } },
            dismissButton = { TextButton(onClick = { showPaste = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}
