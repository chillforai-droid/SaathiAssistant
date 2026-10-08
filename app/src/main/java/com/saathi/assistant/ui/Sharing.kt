package com.saathi.assistant.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.saathi.assistant.workflow.Workflow
import com.saathi.assistant.workflow.WorkflowJson
import java.io.File

/** Writes the workflow JSON to the app cache and opens the Android share sheet. */
fun shareWorkflowFile(context: Context, w: Workflow) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safe = w.name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_').ifEmpty { "workflow" }.take(40)
    val file = File(dir, "$safe.saathi.json")
    file.writeText(WorkflowJson.export(w))
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
