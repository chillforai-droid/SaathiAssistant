@file:OptIn(ExperimentalMaterial3Api::class)

package com.saathi.assistant.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saathi.assistant.R
import com.saathi.assistant.automation.ActionEngine
import com.saathi.assistant.automation.UserPrompt
import com.saathi.assistant.ui.screens.AboutScreen
import com.saathi.assistant.ui.screens.AccessibilitySetupScreen
import com.saathi.assistant.ui.screens.AppProfilesScreen
import com.saathi.assistant.ui.screens.CommandsScreen
import com.saathi.assistant.ui.screens.HistoryScreen
import com.saathi.assistant.ui.screens.HomeScreen
import com.saathi.assistant.ui.screens.PermissionsScreen
import com.saathi.assistant.ui.screens.PrivacyScreen
import com.saathi.assistant.ui.screens.SafetyScreen
import com.saathi.assistant.ui.screens.SettingsScreen
import com.saathi.assistant.ui.screens.WorkflowEditorScreen
import com.saathi.assistant.ui.screens.WorkflowsScreen

object Routes {
    const val HOME = "home"
    const val COMMANDS = "commands"
    const val WORKFLOWS = "workflows"
    const val EDITOR = "editor/{id}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
    const val ACCESSIBILITY = "accessibility"
    const val PROFILES = "profiles"
    const val SAFETY = "safety"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
    fun editor(id: String?) = "editor/${id ?: "new"}"
}

private data class BottomItem(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, R.string.nav_home, Icons.Default.Home),
    BottomItem(Routes.COMMANDS, R.string.nav_commands, Icons.Default.Search),
    BottomItem(Routes.WORKFLOWS, R.string.nav_workflows, Icons.Default.List),
    BottomItem(Routes.HISTORY, R.string.nav_history, Icons.Default.DateRange),
    BottomItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
)

@StringRes
private fun titleFor(route: String?): Int = when {
    route == Routes.HOME -> R.string.app_name
    route == Routes.COMMANDS -> R.string.nav_commands
    route == Routes.WORKFLOWS -> R.string.nav_workflows
    route?.startsWith("editor") == true -> R.string.screen_editor
    route == Routes.HISTORY -> R.string.nav_history
    route == Routes.SETTINGS -> R.string.nav_settings
    route == Routes.PERMISSIONS -> R.string.screen_permissions
    route == Routes.ACCESSIBILITY -> R.string.screen_accessibility
    route == Routes.PROFILES -> R.string.screen_profiles
    route == Routes.SAFETY -> R.string.screen_safety
    route == Routes.PRIVACY -> R.string.screen_privacy
    route == Routes.ABOUT -> R.string.screen_about
    else -> R.string.app_name
}

fun NavController.goTop(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun SaathiApp(vm: MainViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snack = remember { SnackbarHostState() }
    val prompt by vm.prompt.collectAsStateWithLifecycle()
    val engineState by vm.engineState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val isMain = bottomItems.any { it.route == route }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleFor(route))) },
                navigationIcon = {
                    if (!isMain && route != null) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                RunStatusBar(engineState, onCancel = vm::cancel)
                if (isMain) {
                    NavigationBar {
                        bottomItems.forEach { item ->
                            NavigationBarItem(
                                selected = route == item.route,
                                onClick = { nav.goTop(item.route) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.label)) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(vm, nav) }
            composable(Routes.COMMANDS) { CommandsScreen(vm) }
            composable(Routes.WORKFLOWS) { WorkflowsScreen(vm, nav) }
            composable(Routes.EDITOR, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                WorkflowEditorScreen(vm, nav)
            }
            composable(Routes.HISTORY) { HistoryScreen(vm) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
            composable(Routes.PERMISSIONS) { PermissionsScreen(vm, nav) }
            composable(Routes.ACCESSIBILITY) { AccessibilitySetupScreen(vm) }
            composable(Routes.PROFILES) { AppProfilesScreen(vm) }
            composable(Routes.SAFETY) { SafetyScreen() }
            composable(Routes.PRIVACY) { PrivacyScreen() }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }

    PromptDialog(prompt?.prompt, onAnswer = vm::answerPrompt)
}

@Composable
private fun RunStatusBar(state: ActionEngine.State, onCancel: () -> Unit) {
    val running = state as? ActionEngine.State.Running ?: return
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.running_step, running.index, running.total), style = MaterialTheme.typography.labelMedium)
                    Text(running.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.btn_stop)) }
            }
            LinearProgressIndicator(progress = { running.index.toFloat() / running.total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PromptDialog(prompt: UserPrompt?, onAnswer: (Any?) -> Unit) {
    when (prompt) {
        null -> Unit
        is UserPrompt.Confirm -> AlertDialog(
            onDismissRequest = { onAnswer(false) },
            title = { Text(prompt.title) },
            text = { Text(prompt.message) },
            confirmButton = { Button(onClick = { onAnswer(true) }) { Text(prompt.confirmLabel) } },
            dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.btn_cancel)) } }
        )
        is UserPrompt.Choice -> AlertDialog(
            onDismissRequest = { onAnswer(null) },
            title = { Text(prompt.title) },
            text = {
                Column {
                    prompt.options.forEachIndexed { i, option ->
                        TextButton(onClick = { onAnswer(i) }, modifier = Modifier.fillMaxWidth()) { Text(option) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { onAnswer(null) }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}
