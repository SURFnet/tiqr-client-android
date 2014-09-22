package org.tiqr.data;

import android.os.Bundle;

/**
 * Enrollment error.
 */
public class EnrollmentError {
    public enum Type {
        UNKNOWN,
        CONNECTION,
        INVALID_RESPONSE,
        VERIFICATION_REQUIRED,
        USERNAME_TAKEN
    }

    private final Type _type;
    private final String _title;
    private final String _message;
    private final Bundle _extras;

    /**
     * Constructor.
     *
     * @param type
     * @param message
     */
    public EnrollmentError(Type type, String title, String message) {
        this(type, title, message, new Bundle());
    }

    /**
     * Constructor.
     *
     * @param type
     * @param message
     * @param extras
     */
    public EnrollmentError(Type type, String title, String message, Bundle extras) {
        _type = type;
        _title = title;
        _message = message;
        _extras = extras;
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
