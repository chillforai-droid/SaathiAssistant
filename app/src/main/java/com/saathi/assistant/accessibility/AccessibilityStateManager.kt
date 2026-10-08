package com.saathi.assistant.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-wide holder for the connected service instance and its status. */
object AccessibilityStateManager {
    @Volatile var service: SaathiAccessibilityService? = null
        private set

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _currentPackage = MutableStateFlow("")
    val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

    fun onConnected(s: SaathiAccessibilityService) { service = s; _connected.value = true }

    fun onDisconnected(s: SaathiAccessibilityService) {
        if (service === s) { service = null; _connected.value = false; _currentPackage.value = "" }
    }

    fun onPackageChanged(pkg: String) { _currentPackage.value = pkg }

    /** True when the user has switched the service on in Android settings. */
    fun isEnabledInSettings(context: Context): Boolean {
        val cn = ComponentName(context, SaathiAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }
}
