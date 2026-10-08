package com.saathi.assistant.automation

/** Outcome of a single step. [retryable] = worth trying again (e.g. screen not loaded yet). */
data class StepResult(val ok: Boolean, val message: String, val retryable: Boolean = false) {
    companion object {
        fun ok(message: String = "") = StepResult(true, message)
        fun fail(message: String, retryable: Boolean = false) = StepResult(false, message, retryable)
    }
}
