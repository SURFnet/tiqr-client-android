package org.tiqr.service.authentication;

import android.os.Bundle;

/**
 * Authentication error.
 */
public class ParseAuthenticationChallengeError {
    public enum Type {
        UNKNOWN,
        INVALID_CHALLENGE,
        INVALID_IDENTITY_PROVIDER,
        INVALID_IDENTITY,
        NO_IDENTITIES
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
    public ParseAuthenticationChallengeError(Type type, String title, String message) {
        this(type, title, message, new Bundle());
    }

    /**
     * Constructor.
     *
     * @param type
     * @param message
     * @param extras
     */
    public ParseAuthenticationChallengeError(Type type, String title, String message, Bundle extras) {
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
