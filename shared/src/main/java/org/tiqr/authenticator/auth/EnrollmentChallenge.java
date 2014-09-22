package org.tiqr.authenticator.auth;

import android.os.Parcel;

/**
 * Represents an enrollment challenge.
 */
public class EnrollmentChallenge extends Challenge {
    private String _enrollmentURL;


    /**
     * Factory.
     */
    public static final Creator<EnrollmentChallenge> CREATOR = new Creator<EnrollmentChallenge>() {
        public EnrollmentChallenge createFromParcel(Parcel source) {
            return new EnrollmentChallenge(source);
        }

        public EnrollmentChallenge[] newArray(int size) {
            return new EnrollmentChallenge[size];
        }
    };

    /**
     * Constructor.
     */
    public EnrollmentChallenge() {
    }

    /**
     * Constructor.
     */
    private EnrollmentChallenge(Parcel source) {
        super(source);
        _enrollmentURL = source.readString();
    }

    /**
     * Export to parcel.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(_enrollmentURL);
    }

    /**
     * Enrollment callback URL.
     * 
     * @return enrollment callback URL.
     */
    public String getEnrollmentURL() {
        return _enrollmentURL;
    }

    /**
     * Sets the enrollment callback URL for this challenge.
     */
    public void setEnrollmentURL(String enrollmentURL)
    {
        _enrollmentURL = enrollmentURL;
    }
}