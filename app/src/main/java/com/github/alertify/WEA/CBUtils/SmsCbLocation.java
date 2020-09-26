package com.github.alertify.WEA.CBUtils;

import android.os.Parcel;
import android.os.Parcelable;

public class SmsCbLocation implements Parcelable {

    /** The PLMN. Note that this field may be an empty string, but isn't allowed to be null. */
    private final String mPlmn;

    private final int mLac;
    private final int mCid;


    /**
     * Construct a location object for the PLMN. This class is immutable, so
     * the same object can be reused for multiple broadcasts.
     */
    public SmsCbLocation(String plmn) {
        mPlmn = plmn;
        mLac = -1;
        mCid = -1;
    }


    /**
     * Initialize the object from a Parcel.
     */
    public SmsCbLocation(Parcel in) {
        mPlmn = in.readString();
        mLac = in.readInt();
        mCid = in.readInt();
    }


    @Override
    public int hashCode() {
        int hash = mPlmn.hashCode();
        hash = hash * 31 + mLac;
        hash = hash * 31 + mCid;
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SmsCbLocation)) {
            return false;
        }
        SmsCbLocation other = (SmsCbLocation) o;
        return mPlmn.equals(other.mPlmn) && mLac == other.mLac && mCid == other.mCid;
    }


    @Override
    public String toString() {
        return '[' + mPlmn + ',' + mLac + ',' + mCid + ']';
    }



    /**
     * Flatten this object into a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written (ignored).
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPlmn);
        dest.writeInt(mLac);
        dest.writeInt(mCid);
    }

    public static final Creator<SmsCbLocation> CREATOR
            = new Creator<SmsCbLocation>() {
        @Override
        public SmsCbLocation createFromParcel(Parcel in) {
            return new SmsCbLocation(in);
        }

        @Override
        public SmsCbLocation[] newArray(int size) {
            return new SmsCbLocation[size];
        }
    };

    /**
     * Describe the kinds of special objects contained in the marshalled representation.
     * @return a bitmask indicating this Parcelable contains no special objects
     */
    @Override
    public int describeContents() {
        return 0;
    }
}
