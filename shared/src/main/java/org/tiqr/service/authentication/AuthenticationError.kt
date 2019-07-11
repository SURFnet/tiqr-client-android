package org.tiqr.service.authentication

import android.os.Bundle

/**
 * Authentication error.
 */
class AuthenticationError @JvmOverloads constructor(
        /**
         * The exception thrown by the application which triggered this error.
         *
         * @return The exception which was the cause of this error. Can ben null
         */
        val exception: Exception?,
        /**
         * Error type.
         *
         * @return Error type.
         */
        val type: Type,
        /**
         * Title.
         *
         * @return Error title.
         */
        val title: String,
        /**
         * Message.
         *
         * @return Error message.
         */
        val message: String,
        /**
         * Extras.
         *
         * @return Extras.
         */
        val extras: Bundle = Bundle()) {
    enum class Type {
        UNKNOWN,
        CONNECTION,
        INVALID_CHALLENGE,
        INVALID_REQUEST,
        INVALID_RESPONSE,
        INVALID_USER,
        ACCOUNT_BLOCKED,
        ACCOUNT_TEMPORARY_BLOCKED
    }

    constructor(type: Type, title: String, message: String) : this(null, type, title, message, Bundle()) {}

    constructor(type: Type, title: String, message: String, extras: Bundle) : this(null, type, title, message, extras) {}
}
