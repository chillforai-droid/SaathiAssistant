package com.saathi.assistant.util

import android.content.Context
import androidx.annotation.StringRes

fun Context.str(@StringRes id: Int, vararg args: Any): String = getString(id, *args)
