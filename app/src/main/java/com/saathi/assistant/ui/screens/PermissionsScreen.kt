package com.saathi.assistant.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.saathi.assistant.ui.OnResume
import com.saathi.assistant.ui.Routes
import com.saathi.assistant.ui.ScreenColumn

@Composable
fun PermissionsScreen(vm: MainViewModel, nav: NavController) {
    val context = LocalContext.current
    val status by vm.status.collectAsStateWithLifecycle()
    OnResume { vm.refreshStatus() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshStatus() }

    ScreenColumn {
        Text(stringResource(R.string.perm_intro), style = MaterialTheme.typography.bodyMedium)
        PermissionRow(stringResource(R.string.perm_microphone), stringResource(R.string.perm_microphone_why), status.microphone) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
        PermissionRow(stringResource(R.string.perm_contacts), stringResource(R.string.perm_contacts_why), status.contacts) {
            launcher.launch(Manifest.permission.READ_CONTACTS)
        }
        PermissionRow(stringResource(R.string.perm_notifications), stringResource(R.string.perm_notifications_why), status.notifications) {
            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        PermissionRow(stringResource(R.string.perm_accessibility), stringResource(R.string.perm_accessibility_why), status.accessibilityConnected) {
            nav.navigate(Routes.ACCESSIBILITY)
        }
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }) { Text(stringResource(R.string.btn_open_app_settings)) }
    }
}

@Composable
private fun PermissionRow(title: String, why: String, granted: Boolean, onGrant: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(why, style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(if (granted) R.string.status_granted else R.string.status_not_granted),
                    color = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (!granted) Button(onClick = onGrant) { Text(stringResource(R.string.btn_grant)) }
        }
    }
}
