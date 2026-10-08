package com.saathi.assistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saathi.assistant.R
import com.saathi.assistant.data.HistoryEntry
import com.saathi.assistant.ui.MainViewModel
import com.saathi.assistant.ui.ScreenColumn
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val items by vm.history.collectAsStateWithLifecycle()
    ScreenColumn {
        if (items.isEmpty()) Text(stringResource(R.string.history_empty))
        else {
            OutlinedButton(onClick = vm::clearHistory) { Text(stringResource(R.string.btn_clear_history)) }
            items.forEach { HistoryCard(it) }
        }
    }
}

@Composable
private fun HistoryCard(e: HistoryEntry) {
    var expanded by rememberSaveable(e.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(12.dp)) {
            Text(e.command, fontWeight = FontWeight.SemiBold)
            Text(
                (if (e.success) "✓ " else "✗ ") + e.message,
                color = if (e.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(DateFormat.getDateTimeInstance().format(Date(e.time)), style = MaterialTheme.typography.labelSmall)
            if (expanded) e.steps.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
