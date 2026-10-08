package com.saathi.assistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saathi.assistant.R
import com.saathi.assistant.automation.ActionDescriber
import com.saathi.assistant.command.InterpretResult
import com.saathi.assistant.safety.SafetyClassifier
import com.saathi.assistant.safety.SafetyLevel
import com.saathi.assistant.ui.MainViewModel
import com.saathi.assistant.ui.ScreenColumn
import com.saathi.assistant.ui.SectionTitle

private val examples = listOf(
    "WhatsApp खोलो", "व्हाट्सएप खोलो", "open YouTube", "सेटिंग खोलो", "वाईफाई सेटिंग खोलो",
    "Rahul को call करो", "Rahul को फोन करो", "WhatsApp पर Rahul को Hello भेजो",
    "send hi to Rahul on Telegram", "नीचे स्क्रॉल करो", "पीछे जाओ", "home", "screenshot लो",
    "Search पर क्लिक करो", "YouTube Upload Prep चलाओ"
)

@Composable
fun CommandsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<InterpretResult?>(null) }

    ScreenColumn {
        Text(stringResource(R.string.commands_intro), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.command_hint)) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { result = vm.preview(text) }) { Text(stringResource(R.string.btn_preview)) }
            Button(onClick = { vm.submit(text) }) { Text(stringResource(R.string.btn_run)) }
        }
        when (val r = result) {
            null -> Unit
            is InterpretResult.Failure -> Text(vm.errorText(r.error), color = MaterialTheme.colorScheme.error)
            is InterpretResult.Plan -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.preview_steps), style = MaterialTheme.typography.titleSmall)
                    r.plan.steps.forEachIndexed { i, a ->
                        val level = SafetyClassifier.classify(a).level
                        val tag = when (level) {
                            SafetyLevel.SAFE -> stringResource(R.string.level_safe)
                            SafetyLevel.CONFIRM -> stringResource(R.string.level_confirm)
                            SafetyLevel.BLOCK -> stringResource(R.string.level_block)
                        }
                        Text("${i + 1}. ${ActionDescriber.describe(context, a)}  [$tag]", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        SectionTitle(stringResource(R.string.examples))
        examples.forEach { ex ->
            Card(Modifier.fillMaxWidth().clickable { text = ex; result = vm.preview(ex) }) {
                Text(ex, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
