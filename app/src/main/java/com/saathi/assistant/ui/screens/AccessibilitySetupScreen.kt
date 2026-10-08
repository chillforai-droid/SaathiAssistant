package com.saathi.assistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saathi.assistant.R
import com.saathi.assistant.accessibility.AccessibilityActionExecutor
import com.saathi.assistant.ui.InfoCard
import com.saathi.assistant.ui.MainViewModel
import com.saathi.assistant.ui.OnResume
import com.saathi.assistant.ui.ScreenColumn
import com.saathi.assistant.ui.SectionTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccessibilitySetupScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val status by vm.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var inspected by remember { mutableStateOf<List<String>?>(null) }
    var countdown by remember { mutableStateOf(0) }
    OnResume { vm.refreshStatus() }

    ScreenColumn {
        InfoCard(
            stringResource(R.string.acc_status_title),
            stringResource(
                when {
                    status.accessibilityConnected -> R.string.acc_status_connected
                    status.accessibilityEnabled -> R.string.acc_status_enabled_not_connected
                    else -> R.string.acc_status_off
                }
            )
        )
        InfoCard(stringResource(R.string.acc_why_title), stringResource(R.string.acc_why_body))
        InfoCard(stringResource(R.string.acc_steps_title), stringResource(R.string.acc_steps_body))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
            Text(stringResource(R.string.btn_open_accessibility))
        }
        InfoCard(stringResource(R.string.acc_restricted_title), stringResource(R.string.acc_restricted_body))
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }) { Text(stringResource(R.string.btn_open_app_settings)) }

        SectionTitle(stringResource(R.string.acc_inspect_title))
        Text(stringResource(R.string.acc_inspect_body), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            enabled = status.accessibilityConnected && countdown == 0,
            onClick = {
                scope.launch {
                    inspected = null
                    for (i in 5 downTo 1) { countdown = i; delay(1000) }
                    countdown = 0
                    inspected = AccessibilityActionExecutor(context).visibleTexts()
                }
            }
        ) { Text(if (countdown > 0) stringResource(R.string.acc_inspect_wait, countdown) else stringResource(R.string.btn_inspect)) }
        inspected?.let { list ->
            if (list.isEmpty()) Text(stringResource(R.string.acc_inspect_empty))
            else list.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
