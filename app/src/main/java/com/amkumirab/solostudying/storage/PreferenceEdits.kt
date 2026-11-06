package com.amkumirab.solostudying.storage

import android.content.SharedPreferences

/** Writes one synchronous batch and preserves the platform's persistence result. */
internal inline fun SharedPreferences.commitChanges(action: SharedPreferences.Editor.() -> Unit): Boolean =
    edit().apply(action).commit()
