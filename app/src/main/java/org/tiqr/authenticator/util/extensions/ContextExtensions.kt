package org.tiqr.authenticator.util.extensions

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * Get the color from the theme attribute
 */
@ColorInt
fun Context.getThemeColor(@AttrRes attrColor: Int): Int {
    return TypedValue().apply {
        theme.resolveAttribute(attrColor, this, true)
    }.run {
        data
    }
}