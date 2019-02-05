package org.tiqr.service.enrollment;

import android.os.Bundle;

/**
 * Enrollment error.
 */
public class EnrollmentError {
    public enum Type {
        UNKNOWN,
        CONNECTION,
        INVALID_RESPONSE
    }

    private final Type _type;
    private final String _title;
    private final String _message;
    private final Exception _exception;
    private final Bundle _extras;

    /**
     * Constructor.
     *
     * @param type
     * @param message
     */
    public EnrollmentError(Type type, String title, String message, Exception exception) {
        this(type, title, message, new Bundle(), exception);
    }

    /**
     * Constructor.
     *
     * @param type
     * @param message
     * @param extras
     */
    public EnrollmentError(Type type, String title, String message, Bundle extras, Exception exception) {
        _type = type;
        _title = title;
        _message = message;
        _extras = extras;
        _exception = exception;
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

    /**
     * Exception.
     * @return The exception which trigerred this error. Could be null.
     */
    public Exception getException() {
        return _exception;
    }
}
