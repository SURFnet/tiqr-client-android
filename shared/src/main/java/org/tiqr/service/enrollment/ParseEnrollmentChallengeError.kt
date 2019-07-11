package org.tiqr.service.enrollment

import android.os.Bundle

/**
 * Enrollment error.
 */
class ParseEnrollmentChallengeError
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
        CONNECTION,
        INVALID_CHALLENGE,
        INVALID_RESPONSE
    }
}
/**
 * Constructor.
 *
 * @param type
 * @param message
 */
