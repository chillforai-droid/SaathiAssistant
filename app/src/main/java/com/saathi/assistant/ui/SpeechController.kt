package com.saathi.assistant.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Error(val code: Int) : VoiceState
}

/** Thin wrapper over Android's SpeechRecognizer (main thread only). Uses the device's configured recognizer. */
class SpeechController(private val context: Context, private val onFinalResult: (String) -> Unit) {
    private var recognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()
    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial.asStateFlow()

    fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(languageTag: String) {
        if (!isAvailable()) { _state.value = VoiceState.Error(SpeechRecognizer.ERROR_CLIENT); return }
        stop()
        _partial.value = ""
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _state.value = VoiceState.Listening }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) { _state.value = VoiceState.Error(error) }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onPartialResults(partialResults: Bundle?) {
                _partial.value = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }
            override fun onResults(results: Bundle?) {
                _state.value = VoiceState.Idle
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                _partial.value = ""
                if (text.isNotBlank()) onFinalResult(text)
            }
        })
        recognizer = r
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        _state.value = VoiceState.Listening
        r.startListening(intent)
    }

    fun stop() {
        recognizer?.let { runCatching { it.stopListening(); it.cancel(); it.destroy() } }
        recognizer = null
        if (_state.value is VoiceState.Listening) _state.value = VoiceState.Idle
    }

    fun clearError() { if (_state.value is VoiceState.Error) _state.value = VoiceState.Idle }
}
