package org.tiqr.authenticator.auth;

import android.os.Parcel;
import android.os.Parcelable;

import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.datamodel.IdentityProvider;

/**
 * Challenge base class.
 */
public abstract class Challenge implements Parcelable {
    private String _protocolVersion;
    private IdentityProvider _identityProvider;
    private Identity _identity;
    private String _returnURL;

    /**
     * Constructor.
     */
    public Challenge() {

    }

    /**
     * Constructor.
     */
    protected Challenge(Parcel source) {
        _protocolVersion = source.readString();
        _identityProvider = source.readParcelable(IdentityProvider.class.getClassLoader());
        _identity = source.readParcelable(Identity.class.getClassLoader());
        _returnURL = source.readString();
    }

    /**
     * Describe.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Export to parcel.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_protocolVersion);
        dest.writeParcelable(_identityProvider, flags);
        dest.writeParcelable(_identity, flags);
        dest.writeString(_returnURL);
    }

    /**
     * Returns the protocol version.
     *
     * @return Protocol version.
     */
    public String getProtocolVersion() {
        return _protocolVersion;
    }

    /**
     * Sets the used protocol version.
     *
     * @param protocolVersion Protocol version.
     */
    public void setProtocolVersion(String protocolVersion) {
        _protocolVersion = protocolVersion;
    }

    /**
     * Returns the identity provider for this challenge.
     *
     * @return Identity provider
     */
    public IdentityProvider getIdentityProvider()
    {
        return _identityProvider;
    }

    /**
     * Sets the identity provider for this challenge.
     * 
     * @param identityProvider Identity provider.
     */
    public void setIdentityProvider(IdentityProvider identityProvider)
    {
        _identityProvider = identityProvider;
    }

    /**
     * Returns the identity for this challenge, might be null.
     *
     * @return Identity.
     */
    public Identity getIdentity()
    {
        return _identity;
    }

    /**
     * Sets the identity for this challenge.
     */
    public void setIdentity(Identity identity)
    {
        _identity = identity;
    }

    /**
     * Return URL, for example if invoked from a website on the device which wants the user to return
     * to the website after successful authentication.
     *
     * @return return URL.
     */
    public String getReturnURL()
    {
        return _returnURL;
    }

    /**
     * Sets the return URL for this challenge.
     */
    public void setReturnURL(String returnURL)
    {
        _returnURL = returnURL;
    }
}
