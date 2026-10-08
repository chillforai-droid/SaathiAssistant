package com.saathi.assistant.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.saathi.assistant.R
import com.saathi.assistant.ui.InfoCard
import com.saathi.assistant.ui.ScreenColumn

@Composable
fun SafetyScreen() = ScreenColumn {
    InfoCard(stringResource(R.string.safety_safe_title), stringResource(R.string.safety_safe_body))
    InfoCard(stringResource(R.string.safety_confirm_title), stringResource(R.string.safety_confirm_body))
    InfoCard(stringResource(R.string.safety_block_title), stringResource(R.string.safety_block_body))
    InfoCard(stringResource(R.string.safety_note_title), stringResource(R.string.safety_note_body))
}

@Composable
fun PrivacyScreen() = ScreenColumn {
    InfoCard(stringResource(R.string.privacy_acc_title), stringResource(R.string.privacy_acc_body))
    InfoCard(stringResource(R.string.privacy_voice_title), stringResource(R.string.privacy_voice_body))
    InfoCard(stringResource(R.string.privacy_local_title), stringResource(R.string.privacy_local_body))
    InfoCard(stringResource(R.string.privacy_data_title), stringResource(R.string.privacy_data_body))
    InfoCard(stringResource(R.string.privacy_confirm_title), stringResource(R.string.privacy_confirm_body))
}

@Composable
fun AboutScreen() = ScreenColumn {
    InfoCard(stringResource(R.string.app_name), stringResource(R.string.about_body))
    InfoCard(stringResource(R.string.about_limits_title), stringResource(R.string.about_limits_body))
    InfoCard(stringResource(R.string.about_version_title), "1.0.0")
}
