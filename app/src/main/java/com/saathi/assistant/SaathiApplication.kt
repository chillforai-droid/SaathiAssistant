package com.saathi.assistant

import android.app.Application
import com.saathi.assistant.adapters.AdapterRegistry
import com.saathi.assistant.adapters.SendConfirmText
import com.saathi.assistant.automation.ActionEngine
import com.saathi.assistant.automation.AppLauncher
import com.saathi.assistant.command.CommandInterpreter
import com.saathi.assistant.command.CommandParser
import com.saathi.assistant.command.RuleBasedCommandInterpreter
import com.saathi.assistant.data.ContactResolver
import com.saathi.assistant.data.HistoryRepository
import com.saathi.assistant.data.SettingsStore
import com.saathi.assistant.util.Notifier
import com.saathi.assistant.workflow.WorkflowRepository
import java.io.File

/** Manual dependency container — no DI framework, no network, no analytics. */
class AppContainer(app: Application) {
    val settings = SettingsStore(app)
    val workflows = WorkflowRepository(File(app.filesDir, "workflows.json"))
    val history = HistoryRepository(File(app.filesDir, "history.json"))
    val launcher = AppLauncher(app)
    val contacts = ContactResolver(app)
    val adapters = AdapterRegistry()
    val engine = ActionEngine(app, launcher, contacts, adapters, { workflows.workflows.value }, settings)
    val interpreter: CommandInterpreter = RuleBasedCommandInterpreter(
        CommandParser(),
        { workflows.workflows.value },
        adapters,
        SendConfirmText { contact, appName, message -> app.getString(R.string.confirm_send_body, contact, appName, message) }
    )
}

class SaathiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannel(this)
    }
}
