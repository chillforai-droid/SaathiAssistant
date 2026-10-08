package com.saathi.assistant.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saathi.assistant.R
import com.saathi.assistant.automation.labelRes
import com.saathi.assistant.ui.MainViewModel
import com.saathi.assistant.ui.OnResume
import com.saathi.assistant.ui.ScreenColumn

@Composable
fun AppProfilesScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val c = vm.container
    ScreenColumn {
        Text(stringResource(R.string.profiles_intro), style = MaterialTheme.typography.bodyMedium)
        c.adapters.adapters.forEach { adapter ->
            val resolved = remember(adapter.id) { c.launcher.resolve(adapter.app.key) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${adapter.app.emoji} ${adapter.app.displayName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (resolved != null) stringResource(R.string.profile_installed, resolved.packageName) else stringResource(R.string.profile_not_installed),
                        color = if (resolved != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(stringResource(adapter.notesRes), style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(R.string.profile_actions, adapter.supportedActions().joinToString(", ") { context.getString(it.labelRes()) }),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
