package com.saathi.assistant.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.saathi.assistant.accessibility.AccessibilityStateManager

data class SystemStatus(
    val microphone: Boolean = false,
    val contacts: Boolean = false,
    val notifications: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val accessibilityConnected: Boolean = false
)

object PermissionHelper {
    private fun granted(context: Context, p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    fun status(context: Context) = SystemStatus(
        microphone = granted(context, Manifest.permission.RECORD_AUDIO),
        contacts = granted(context, Manifest.permission.READ_CONTACTS),
        notifications = Build.VERSION.SDK_INT < 33 || granted(context, Manifest.permission.POST_NOTIFICATIONS),
        accessibilityEnabled = AccessibilityStateManager.isEnabledInSettings(context),
        accessibilityConnected = AccessibilityStateManager.connected.value
    )
}
