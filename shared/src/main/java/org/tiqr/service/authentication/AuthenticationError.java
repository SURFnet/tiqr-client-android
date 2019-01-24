package org.tiqr.service.authentication;

import android.os.Bundle;

/**
 * Authentication error.
 */
public class AuthenticationError {
    public enum Type {
        UNKNOWN,
        CONNECTION,
        INVALID_CHALLENGE,
        INVALID_REQUEST,
        INVALID_RESPONSE,
        INVALID_USER,
        ACCOUNT_BLOCKED,
        ACCOUNT_TEMPORARY_BLOCKED
    }

    private final Exception _exception;
    private final Type _type;
    private final String _title;
    private final String _message;
    private final Bundle _extras;

    public AuthenticationError(Type type, String title, String message) {
        this(null, type, title, message, new Bundle());
    }

    public AuthenticationError(Exception exception, Type type, String title, String message) {
        this(exception, type, title, message, new Bundle());
    }

    public AuthenticationError(Type type, String title, String message, Bundle extras) {
        this(null, type, title, message, extras);
    }


    public AuthenticationError(Exception exception, Type type, String title, String message, Bundle extras) {
        _exception = exception;
        _type = type;
        _title = title;
        _message = message;
        _extras = extras;
    }

    /**
     * The exception thrown by the application which triggered this error.
     *
     * @return The exception which was the cause of this error. Can ben null
     */
    public Exception getException() {
        return _exception;
    }

    /**
     * Error type.
     *
     * @return Error type.
     */
    public Type getType() {
        return _type;
    }

    /**
     * Title.
     *
     * @return Error title.
     */
    public String getTitle() {
        return _title;
    }

    /**
     * Message.
     *
     * @return Error message.
     */
    public String getMessage() {
        return _message;
    }

    /**
     * Extras.
     *
     * @return Extras.
     */
    public Bundle getExtras() {
        return _extras;
    }
}
