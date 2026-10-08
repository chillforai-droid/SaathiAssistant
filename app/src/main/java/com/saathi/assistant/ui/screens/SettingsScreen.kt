package com.saathi.assistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.saathi.assistant.R
import com.saathi.assistant.ui.LinkRow
import com.saathi.assistant.ui.MainViewModel
import com.saathi.assistant.ui.Routes
import com.saathi.assistant.ui.ScreenColumn
import com.saathi.assistant.ui.SectionTitle
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel, nav: NavController) {
    val context = LocalContext.current
    val s = vm.settings
    val lang by s.speechLang.collectAsStateWithLifecycle()
    val timeout by s.timeoutSec.collectAsStateWithLifecycle()
    val retry by s.retryLimit.collectAsStateWithLifecycle()

    ScreenColumn {
        SectionTitle(stringResource(R.string.set_speech_language))
        listOf("hi-IN" to R.string.lang_hindi, "en-IN" to R.string.lang_english).forEach { (tag, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = lang == tag, onClick = { s.setSpeechLang(tag) })
                Text(stringResource(label), Modifier.padding(start = 4.dp))
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            }) { Text(stringResource(R.string.set_app_language)) }
        }

        SectionTitle(stringResource(R.string.set_step_timeout, timeout))
        Slider(value = timeout.toFloat(), onValueChange = { s.setTimeoutSec(it.roundToInt()) }, valueRange = 3f..30f)
        SectionTitle(stringResource(R.string.set_retry_limit, retry))
        Slider(value = retry.toFloat(), onValueChange = { s.setRetryLimit(it.roundToInt()) }, valueRange = 0f..5f, steps = 4)

        SectionTitle(stringResource(R.string.set_more))
        LinkRow(stringResource(R.string.screen_permissions), stringResource(R.string.perm_screen_sub)) { nav.navigate(Routes.PERMISSIONS) }
        LinkRow(stringResource(R.string.screen_accessibility), stringResource(R.string.acc_screen_sub)) { nav.navigate(Routes.ACCESSIBILITY) }
        LinkRow(stringResource(R.string.screen_profiles), stringResource(R.string.profiles_screen_sub)) { nav.navigate(Routes.PROFILES) }
        LinkRow(stringResource(R.string.screen_safety), stringResource(R.string.safety_screen_sub)) { nav.navigate(Routes.SAFETY) }
        LinkRow(stringResource(R.string.screen_privacy), stringResource(R.string.privacy_screen_sub)) { nav.navigate(Routes.PRIVACY) }
        LinkRow(stringResource(R.string.screen_about)) { nav.navigate(Routes.ABOUT) }
    }
}
