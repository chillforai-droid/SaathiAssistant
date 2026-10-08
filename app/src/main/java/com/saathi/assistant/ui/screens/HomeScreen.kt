package com.saathi.assistant.ui.screens

import android.Manifest
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.saathi.assistant.R
import com.saathi.assistant.adapters.KnownApps
import com.saathi.assistant.command.CommandSource
import com.saathi.assistant.ui.MainViewModel
import com.saathi.assistant.ui.OnResume
import com.saathi.assistant.ui.Routes
import com.saathi.assistant.ui.ScreenColumn
import com.saathi.assistant.ui.SectionTitle
import com.saathi.assistant.ui.VoiceState

private val quickKeys = listOf(
    "whatsapp", "youtube", "messenger", "facebook", "instagram", "telegram",
    "chrome", "camera", "gallery", "files", "phone", "settings"
)

@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val text by vm.commandText.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val voice by vm.voice.collectAsStateWithLifecycle()
    val partial by vm.partial.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val workflows by vm.workflows.collectAsStateWithLifecycle()
    OnResume { vm.refreshStatus() }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.refreshStatus()
        if (granted) vm.startListening()
    }
    val onMic = {
        when {
            voice is VoiceState.Listening -> vm.stopListening()
            status.microphone -> { vm.clearVoiceError(); vm.startListening() }
            else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Text("S", color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            Column {
                Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusCard(
                Modifier.weight(1f), stringResource(R.string.perm_accessibility),
                if (status.accessibilityConnected) stringResource(R.string.status_on) else stringResource(R.string.status_off),
                status.accessibilityConnected
            ) { nav.navigate(Routes.ACCESSIBILITY) }
            StatusCard(
                Modifier.weight(1f), stringResource(R.string.perm_microphone),
                if (status.microphone) stringResource(R.string.status_granted) else stringResource(R.string.status_not_granted),
                status.microphone
            ) { nav.navigate(Routes.PERMISSIONS) }
        }

        // Microphone button
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val listening = voice is VoiceState.Listening
            Box(
                Modifier.size(96.dp).clip(CircleShape)
                    .background(if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    .clickable(onClick = { onMic() }),
                contentAlignment = Alignment.Center
            ) { Text(if (listening) "⏹" else "🎤", fontSize = 42.sp, color = Color.White) }
            Text(
                when (val v = voice) {
                    VoiceState.Idle -> stringResource(R.string.mic_hint)
                    VoiceState.Listening -> partial.ifBlank { stringResource(R.string.mic_listening) }
                    is VoiceState.Error -> stringResource(voiceErrorRes(v.code))
                },
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text, onValueChange = vm::setCommand, modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.command_hint)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { vm.submit(text) })
            )
            Button(onClick = { vm.submit(text) }) { Text(stringResource(R.string.btn_run)) }
        }

        SectionTitle(stringResource(R.string.quick_actions))
        quickKeys.chunked(4).forEach { rowKeys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowKeys.forEach { key ->
                    val app = KnownApps.byKey(key)
                    Column(
                        Modifier.weight(1f).clip(MaterialTheme.shapes.medium).clickable { vm.quickOpen(key) }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(app?.emoji ?: "⚙️", fontSize = 28.sp)
                        Text(app?.displayName ?: stringResource(R.string.nav_settings), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        val favorites = workflows.filter { it.favorite && it.enabled }
        if (favorites.isNotEmpty()) {
            SectionTitle(stringResource(R.string.favorite_workflows))
            favorites.forEach { w ->
                FilledTonalButton(onClick = { vm.runWorkflow(w.id) }, modifier = Modifier.fillMaxWidth()) { Text("▶ ${w.name}") }
            }
        }

        if (recent.isNotEmpty()) {
            SectionTitle(stringResource(R.string.recent_commands))
            recent.forEach { cmd ->
                OutlinedButton(onClick = { vm.submit(cmd, CommandSource.TEXT) }, modifier = Modifier.fillMaxWidth()) { Text(cmd, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun StatusCard(modifier: Modifier, title: String, value: String, ok: Boolean, onClick: () -> Unit) {
    Card(
        modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (ok) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text((if (ok) "✓ " else "! ") + value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

fun voiceErrorRes(code: Int): Int = when (code) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.voice_err_nomatch
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_err_permission
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.voice_err_network
    SpeechRecognizer.ERROR_CLIENT -> R.string.voice_err_unavailable
    else -> R.string.voice_err_generic
}
