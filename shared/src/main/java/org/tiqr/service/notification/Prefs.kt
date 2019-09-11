package org.tiqr.service.notification

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    operator fun get(context: Context): SharedPreferences {
        return context.getSharedPreferences("SA_PREFS", 0)
    }
}