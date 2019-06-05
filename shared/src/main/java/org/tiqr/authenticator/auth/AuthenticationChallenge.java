package org.tiqr.authenticator.auth;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an authentication challenge.
 */
public class AuthenticationChallenge extends Challenge implements Parcelable {
    private String _sessionKey;
    private String _challenge;
    private String _serviceProviderDisplayName;
    private String _serviceProviderIdentifier;
    private boolean _isStepUpChallenge;

    /**
     * Factory.
     */
    public static final Creator<AuthenticationChallenge> CREATOR = new Creator<AuthenticationChallenge>() {
        public AuthenticationChallenge createFromParcel(Parcel source) {
            return new AuthenticationChallenge(source);
        }

        public AuthenticationChallenge[] newArray(int size) {
            return new AuthenticationChallenge[size];
        }
    };

    /**
     * Constructor.
     */
    public AuthenticationChallenge() {
    }

    /**
     * Constructor.
     */
    private AuthenticationChallenge(Parcel source) {
        super(source);
        _sessionKey = source.readString();
        _challenge = source.readString();
        _serviceProviderDisplayName = source.readString();
        _serviceProviderIdentifier = source.readString();
        _isStepUpChallenge = source.readInt() == 1;
    }

    /**
     * Export to parcel.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(_sessionKey);
        dest.writeString(_challenge);
        dest.writeString(_serviceProviderDisplayName);
        dest.writeString(_serviceProviderIdentifier);
        dest.writeInt(_isStepUpChallenge ? 1 : 0);
    }

    /**
     * The session key for this challenge.
     *
     * @return session key
     */
    public String getSessionKey() {
        return _sessionKey;
    }

    /**
     * Sets the session key for this challenge.
     *
     * @param sessionKey session key
     */
    public void setSessionKey(String sessionKey) {
        _sessionKey = sessionKey;
    }

    /**
     * The authentication challenge, used to verify the request (not to be confused with the
     * raw challenge!).
     *
     * @return authentication challenge
     */
    public String getChallenge() {
        return _challenge;
    }

    /**
     * Sets the authentication challenge, used to verify the request (not to be confused with the
     * raw challenge!).
     *
     * @param challenge authentication challenge
     */
    public void setChallenge(String challenge) {
        _challenge = challenge;
    }

    /**
     * The service provider (readable) name
     *
     * @return the service provider
     */
    public String getServiceProviderDisplayName() {
        return _serviceProviderDisplayName;
    }

    /**
     * Sets the service provider display name.
     *
     * @param serviceProviderDisplayName the service provider display name to set
     */
    public void setServiceProviderDisplayName(String serviceProviderDisplayName) {
        _serviceProviderDisplayName = serviceProviderDisplayName;
    }

    /**
     * A unique identifier for the service provider
     *
     * @return the service provider identifier
     */
    public String getServiceProviderIdentifier() {
        return _serviceProviderIdentifier;
    }

    /**
     * Sets the service provider identifier.
     *
     * @param serviceProviderIdentifier The service provider identifier to set
     */
    public void setServiceProviderIdentifier(String serviceProviderIdentifier) {
        _serviceProviderIdentifier = serviceProviderIdentifier;
    }

    /**
     * Returns if the authentication is a step-up challenge.
     *
     * @return True if the server already knows the identity of the user. False if not.
     */
    public boolean isStepUpChallenge() {
        return _isStepUpChallenge;
    }

    /**
     * Sets if the authentication is a step-up challenge.
     *
     * @param stepUpChallenge True if the server already knows the identity of the user. False if not.
     */
    public void setStepUpChallenge(boolean stepUpChallenge) {
        _isStepUpChallenge = stepUpChallenge;
    }
}
