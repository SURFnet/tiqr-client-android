package org.tiqr.authenticator.datamodel;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Identity wrapper class for identities.
 */
public class Identity implements Parcelable {
    private long _id = -1;
    private String _identifier;
    private String _displayName;
    private int _sortIndex = 0;
    private boolean _blocked = false;
    private boolean _showFingerprintUpgrade = true;
    private boolean _useFingerprint= false;

    /**
     * Factory.
     */
    public static final Parcelable.Creator<Identity> CREATOR = new Parcelable.Creator<Identity>() {
        public Identity createFromParcel(Parcel source) {
            return new Identity(source);
        }

        public Identity[] newArray(int size) {
            return new Identity[size];
        }
    };

    /**
     * Constructor.
     */
    public Identity() {
    }

    /**
     * Constructor.
     *
     * @param source
     */
    private Identity(Parcel source) {
        _id = source.readLong();
        _identifier = source.readString();
        _displayName = source.readString();
        _sortIndex = source.readInt();
        _blocked = source.readByte() != 0;
        _showFingerprintUpgrade = source.readByte() != 0;
        _useFingerprint = source.readByte() != 0;
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
        dest.writeLong(_id);
        dest.writeString(_identifier);
        dest.writeString(_displayName);
        dest.writeInt(_sortIndex);
        dest.writeByte(_blocked ? (byte) 1 : (byte) 0);
        dest.writeByte(_showFingerprintUpgrade ? (byte)1 : (byte)0);
        dest.writeByte(_useFingerprint ? (byte)1 : (byte)0);
    }

    /**
     * Is this a new service?
     * 
     * @return boolean is new? 
     */
    public boolean isNew()
    {
        return _id == -1;
    }
    
    /**
     * Returns the identity (row) id.
     * 
     * The id is -1 for an identity that hasn't bee inserted yet.
     * 
     * @return identity id
     */
    public long getId()
    {
        return _id;
    }
    
    /**
     * Sets the identity row id.
     * 
     * @param id row id
     */
    public void setId(long id)
    {
        _id = id;
    }
    
    /**
     * Returns the service identifier.
     * 
     * @return service identifier
     */
    public String getIdentifier()
    {
        return _identifier;
    }
    
    /**
     * Sets the identifier.
     * 
     * @param identifier identifier
     */
    public void setIdentifier(String identifier)
    {
        _identifier = identifier;
    }
    
    /**
     * Returns the display name.
     * 
     * @return display name
     */
    public String getDisplayName()
    {
        return _displayName;
    }
    
    /**
     * Sets the display name.
     * 
     * @param displayName display name
     */
    public void setDisplayName(String displayName)
    {
        _displayName = displayName;
    }    
    
    /**
     * Returns the sort index.
     * 
     * @return sort index
     */
    public int getSortIndex()
    {
        return _sortIndex;
    }
    
    /**
     * Sets the sort index.
     * 
     * @param sortIndex sort index
     */
    public void setSortIndex(int sortIndex)
    {
        _sortIndex = sortIndex;
    }

	/**
	 * Block (or unblock) a user
	 * 
	 * @param blocked blocked or not
	 */
	public void setBlocked(boolean blocked) {
		_blocked = blocked;
	}

	/**
	 * Whether the user is blocked or not
	 * 
	 * @return boolean blocked or not 
	 */
	public boolean isBlocked() {
		return _blocked;
	}

    public boolean showFingerprintUpgrade() {
        return _showFingerprintUpgrade;
    }

    public void setShowFingerprintUpgrade(boolean showFingerprintUpgrade) {
        _showFingerprintUpgrade = showFingerprintUpgrade;
    }

    public boolean isUsingFingerprint() {
        return _useFingerprint;
    }

    public void setUseFingerprint(boolean useFingerprint) {
        _useFingerprint = useFingerprint;
    }


}