package org.tiqr.authenticator.exceptions

/**
 * An exception that should be shown to the user in an alert dialog.
 *
 * The exception uses a resource string as it's message.
 */
class UserException : Exception {

    constructor(message: String) : super(message) {}

    constructor(message: String, parent: Throwable) : super(message, parent) {}

    companion object {
        private val serialVersionUID = 2999071347338101165L
    }
}
