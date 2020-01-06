package org.tiqr.service

sealed class Token {
    data class Valid(val value: String) : Token()
    object Invalid : Token()
}
