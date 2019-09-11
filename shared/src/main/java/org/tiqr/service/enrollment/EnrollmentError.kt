package org.tiqr.service.enrollment

import android.os.Bundle

/**
 * Enrollment error.
 */
class EnrollmentError
/**
 * Constructor.
 *
 * @param type
 * @param message
 * @param extras
 */
(
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
        val extras: Bundle,
        /**
         * Exception.
         * @return The exception which trigerred this error. Could be null.
         */
        val exception: Exception) {
    enum class Type {
        UNKNOWN,
        CONNECTION,
        INVALID_RESPONSE
    }

    /**
     * Constructor.
     *
     * @param type
     * @param message
     */
    constructor(type: Type, title: String, message: String, exception: Exception) : this(type, title, message, Bundle(), exception) {}
}
