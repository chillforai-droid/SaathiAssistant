package com.saathi.assistant.automation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.saathi.assistant.adapters.KnownApps

/** Resolves and launches apps / system screens. Never assumes a package is installed. */
class AppLauncher(private val context: Context) {

    data class Resolved(val label: String, val packageName: String, val intent: Intent)

    private val pm get() = context.packageManager

    fun resolve(target: String): Resolved? {
        val t = target.trim()
        if (t.isEmpty()) return null
        KnownApps.byKey(t)?.let { app ->
            for (pkg in app.packages) {
                pm.getLaunchIntentForPackage(pkg)?.let { return Resolved(app.displayName, pkg, it) }
            }
            app.launchAction?.let { action ->
                val i = Intent(action)
                val pkg = i.resolveActivity(pm)?.packageName
                if (pkg != null) return Resolved(app.displayName, pkg, i)
            }
            return null
        }
        pm.getLaunchIntentForPackage(t)?.let { return Resolved(labelOf(t), t, it) }
        // Fall back to matching an installed app's visible name (exact, or one unique partial match).
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        val exact = apps.firstOrNull { it.loadLabel(pm).toString().equals(t, ignoreCase = true) }
        val chosen = exact ?: apps.filter { it.loadLabel(pm).toString().contains(t, ignoreCase = true) }.let { if (it.size == 1) it[0] else null }
        chosen?.let { ri ->
            val pkg = ri.activityInfo.packageName
            pm.getLaunchIntentForPackage(pkg)?.let { return Resolved(ri.loadLabel(pm).toString(), pkg, it) }
        }
        return null
    }

    private fun labelOf(pkg: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    fun launch(r: Resolved): Boolean = start(Intent(r.intent))

    fun openUri(uri: String): Boolean = start(Intent(Intent.ACTION_VIEW, Uri.parse(uri.trim())))

    fun dial(number: String): Boolean = start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))

    fun share(text: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        return start(Intent.createChooser(send, null))
    }

    fun openSettings(key: String): Boolean {
        val action = when (key.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Intent.ACTION_POWER_USAGE_SUMMARY
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            "security" -> Settings.ACTION_SECURITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return start(Intent(action))
    }
}
