package com.saathi.assistant.automation

sealed interface UserPrompt {
    val title: String
    data class Confirm(override val title: String, val message: String, val confirmLabel: String) : UserPrompt
    data class Choice(override val title: String, val options: List<String>) : UserPrompt
}

/** The engine asks the user through this interface; the UI layer implements it. */
interface UserInteraction {
    suspend fun confirm(prompt: UserPrompt.Confirm): Boolean
    /** Returns the chosen index, or null if cancelled. */
    suspend fun choose(prompt: UserPrompt.Choice): Int?
}
