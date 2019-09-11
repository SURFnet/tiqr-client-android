package org.tiqr.service.authentication

import android.os.Bundle

/**
 * Authentication error.
 */
class ParseAuthenticationChallengeError
/**
 * Constructor.
 *
 * @param type
 * @param message
 * @param extras
 */
@JvmOverloads constructor(
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
        INVALID_CHALLENGE,
        INVALID_IDENTITY_PROVIDER,
        INVALID_IDENTITY,
        NO_IDENTITIES
    }
}
/**
 * Constructor.
 *
 * @param type
 * @param message
 */
