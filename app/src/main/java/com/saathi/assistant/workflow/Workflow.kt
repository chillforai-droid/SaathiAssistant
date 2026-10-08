package com.saathi.assistant.workflow

import com.saathi.assistant.automation.Action
import com.saathi.assistant.util.newId
import kotlinx.serialization.Serializable

@Serializable
data class Workflow(
    val id: String = newId(),
    val name: String,
    val description: String = "",
    val triggerPhrases: List<String> = emptyList(),
    val actions: List<Action> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val enabled: Boolean = true,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
