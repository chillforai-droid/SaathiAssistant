package com.saathi.assistant.adapters

import androidx.annotation.StringRes
import com.saathi.assistant.R
import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType
import com.saathi.assistant.automation.StepResult
import com.saathi.assistant.command.ActionPlan

/** Delegates core actions to the generic adapter; subclasses add app-specific knowledge. */
abstract class BaseAdapter(private val generic: GenericAdapter, override val app: KnownApp) : AppAdapter {
    override val id: String get() = app.key
    override fun canHandle(packageName: String) = packageName in app.packages
    override suspend fun executeAction(action: Action, env: AdapterEnv): StepResult = generic.executeAction(action, env)
    @get:StringRes abstract val notesRes: Int
    open val supportsSendFlow: Boolean get() = false
}

/** Apps where we only guarantee "open" plus generic accessibility actions (click/type/scroll). */
class ProfileAdapter(generic: GenericAdapter, app: KnownApp, @StringRes override val notesRes: Int) : BaseAdapter(generic, app) {
    override fun supportedActions(): Set<ActionType> = setOf(
        ActionType.OPEN_APP, ActionType.CLICK_TEXT, ActionType.CLICK_ID, ActionType.TYPE_TEXT,
        ActionType.SCROLL, ActionType.BACK, ActionType.HOME, ActionType.WAIT, ActionType.SHARE
    )
}

/**
 * Guided chat flow shared by WhatsApp / Messenger / Telegram:
 * open app -> search -> type contact -> open chat -> type message -> CONFIRM -> send.
 * Button labels differ per app version and language, so every step can fail; when it does the
 * engine stops and reports the reason instead of pretending it worked.
 */
class MessagingAdapter(
    generic: GenericAdapter,
    app: KnownApp,
    private val searchLabels: String,
    private val sendLabels: String = "Send|भेजें|SEND",
    @StringRes override val notesRes: Int
) : BaseAdapter(generic, app) {

    override val supportsSendFlow: Boolean get() = true

    override fun supportedActions(): Set<ActionType> = setOf(
        ActionType.OPEN_APP, ActionType.CLICK_TEXT, ActionType.CLICK_ID, ActionType.TYPE_TEXT,
        ActionType.SCROLL, ActionType.BACK, ActionType.HOME, ActionType.WAIT, ActionType.SHARE
    )

    override fun buildSendMessagePlan(contact: String, message: String, confirm: SendConfirmText): ActionPlan =
        ActionPlan(
            title = "${app.displayName}: $contact",
            steps = listOf(
                Action(type = ActionType.OPEN_APP, target = app.key),
                Action(type = ActionType.WAIT, delayMs = 2500),
                Action(type = ActionType.CLICK_TEXT, target = searchLabels),
                Action(type = ActionType.WAIT, delayMs = 800),
                Action(type = ActionType.TYPE_TEXT, value = contact),
                Action(type = ActionType.WAIT, delayMs = 1500),
                Action(type = ActionType.CLICK_TEXT, target = contact),
                Action(type = ActionType.WAIT, delayMs = 1500),
                Action(type = ActionType.TYPE_TEXT, value = message),
                Action(
                    type = ActionType.CLICK_TEXT,
                    target = sendLabels,
                    value = confirm.text(contact, app.displayName, message),
                    confirmationRequired = true
                )
            )
        )
}

class AdapterRegistry {
    private val generic = GenericAdapter()

    private fun app(key: String) = requireNotNull(KnownApps.byKey(key)) { "Unknown app key $key" }

    val adapters: List<BaseAdapter> = listOf(
        MessagingAdapter(generic, app("whatsapp"), "Search|खोजें|सर्च", notesRes = R.string.note_whatsapp),
        MessagingAdapter(generic, app("messenger"), "Search|खोजें|सर्च", notesRes = R.string.note_messenger),
        MessagingAdapter(generic, app("telegram"), "Search|खोजें|सर्च", notesRes = R.string.note_telegram),
        ProfileAdapter(generic, app("youtube"), R.string.note_youtube),
        ProfileAdapter(generic, app("youtube_studio"), R.string.note_youtube),
        ProfileAdapter(generic, app("facebook"), R.string.note_facebook),
        ProfileAdapter(generic, app("instagram"), R.string.note_instagram),
        ProfileAdapter(generic, app("chrome"), R.string.note_chrome),
        ProfileAdapter(generic, app("gallery"), R.string.note_gallery),
        ProfileAdapter(generic, app("files"), R.string.note_files)
    )

    fun forPackage(pkg: String): AppAdapter = adapters.firstOrNull { it.canHandle(pkg) } ?: generic
    fun forKey(key: String): AppAdapter? = adapters.firstOrNull { it.app.key.equals(key, true) }
}
