package com.saathi.assistant.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactMatch(val name: String, val number: String)

/** Looks contacts up locally on the device. Nothing is stored or uploaded. */
class ContactResolver(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** Distinct (name, number) pairs whose display name contains [query]. */
    fun find(query: String): List<ContactMatch> {
        if (!hasPermission()) return emptyList()
        val q = query.trim().replace("%", "").replace("_", "")
        if (q.isEmpty()) return emptyList()
        val result = LinkedHashMap<String, ContactMatch>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$q%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { c ->
                while (c.moveToNext() && result.size < 20) {
                    val name = c.getString(0).orEmpty()
                    val number = c.getString(1).orEmpty()
                    val digits = number.filter { it.isDigit() }
                    if (digits.length < 3) continue
                    result.putIfAbsent(digits.takeLast(10), ContactMatch(name, number.trim()))
                }
            }
        }
        return result.values.toList()
    }
}
