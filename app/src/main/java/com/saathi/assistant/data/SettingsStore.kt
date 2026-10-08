package com.saathi.assistant.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small user settings kept in app-private SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("saathi_settings", Context.MODE_PRIVATE)

    private val _speechLang = MutableStateFlow(prefs.getString(K_LANG, "hi-IN") ?: "hi-IN")
    val speechLang: StateFlow<String> = _speechLang.asStateFlow()

    private val _timeoutSec = MutableStateFlow(prefs.getInt(K_TIMEOUT, 10))
    val timeoutSec: StateFlow<Int> = _timeoutSec.asStateFlow()

    private val _retryLimit = MutableStateFlow(prefs.getInt(K_RETRY, 2))
    val retryLimit: StateFlow<Int> = _retryLimit.asStateFlow()

    fun setSpeechLang(v: String) { prefs.edit().putString(K_LANG, v).apply(); _speechLang.value = v }
    fun setTimeoutSec(v: Int) { val x = v.coerceIn(3, 30); prefs.edit().putInt(K_TIMEOUT, x).apply(); _timeoutSec.value = x }
    fun setRetryLimit(v: Int) { val x = v.coerceIn(0, 5); prefs.edit().putInt(K_RETRY, x).apply(); _retryLimit.value = x }

    private companion object {
        const val K_LANG = "speech_lang"
        const val K_TIMEOUT = "step_timeout_sec"
        const val K_RETRY = "retry_limit"
    }
}
