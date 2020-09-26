package com.github.alertify.WEA.CBUtils;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public final class SmsCbMessage implements Parcelable {
    /** Cell wide geographical scope with immediate display (GSM/UMTS only). */
    public static final int GEOGRAPHICAL_SCOPE_CELL_WIDE_IMMEDIATE = 0;
    /** PLMN wide geographical scope (GSM/UMTS and all CDMA broadcasts). */
    public static final int GEOGRAPHICAL_SCOPE_PLMN_WIDE = 1;
    /** Location / service area wide geographical scope (GSM/UMTS only). */
    public static final int GEOGRAPHICAL_SCOPE_LOCATION_AREA_WIDE = 2;
    /** Cell wide geographical scope (GSM/UMTS only). */
    public static final int GEOGRAPHICAL_SCOPE_CELL_WIDE = 3;



    @IntDef(prefix = { "GEOGRAPHICAL_SCOPE_" }, value = {
            GEOGRAPHICAL_SCOPE_CELL_WIDE_IMMEDIATE,
            GEOGRAPHICAL_SCOPE_PLMN_WIDE,
            GEOGRAPHICAL_SCOPE_LOCATION_AREA_WIDE,
            GEOGRAPHICAL_SCOPE_CELL_WIDE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface GeographicalScope {}
    /** GSM or UMTS format cell broadcast. */
    public static final int MESSAGE_FORMAT_3GPP = 1;
    /** CDMA format cell broadcast. */
    public static final int MESSAGE_FORMAT_3GPP2 = 2;

    @IntDef(prefix = { "MESSAGE_FORMAT_" }, value = {
            MESSAGE_FORMAT_3GPP,
            MESSAGE_FORMAT_3GPP2
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MessageFormat {}
    /** Normal message priority. */
    public static final int MESSAGE_PRIORITY_NORMAL = 0;
    /** Interactive message priority. */
    public static final int MESSAGE_PRIORITY_INTERACTIVE = 1;
    /** Urgent message priority. */
    public static final int MESSAGE_PRIORITY_URGENT = 2;
    /** Emergency message priority. */
    public static final int MESSAGE_PRIORITY_EMERGENCY = 3;

    public static final int MAXIMUM_WAIT_TIME_NOT_SET = 255;

    @IntDef(prefix = { "MESSAGE_PRIORITY_" }, value = {
            MESSAGE_PRIORITY_NORMAL,
            MESSAGE_PRIORITY_INTERACTIVE,
            MESSAGE_PRIORITY_URGENT,
            MESSAGE_PRIORITY_EMERGENCY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MessagePriority {}

    /** Format of this message (for interpretation of service category values). */
    private final int mMessageFormat;
    /** Geographical scope of broadcast. */
    private final int mGeographicalScope;
    /**
     * Serial number of broadcast (message identifier for CDMA, geographical scope + message code +
     * update number for GSM/UMTS). The serial number plus the location code uniquely identify
     * a cell broadcast for duplicate detection.
     */
    private final int mSerialNumber;
    /**
     * Location identifier for this message. It consists of the current operator MCC/MNC as a
     * 5 or 6-digit decimal string. In addition, for GSM/UMTS, if the Geographical Scope of the
     * message is not binary 01, the Location Area is included for comparison. If the GS is
     * 00 or 11, the Cell ID is also included. LAC and Cell ID are -1 if not specified.
     */
    @NonNull
    private final SmsCbLocation mLocation;
    /**
     * 16-bit CDMA service category or GSM/UMTS message identifier. For ETWS and CMAS warnings,
     * the information provided by the category is also available via {@link #getEtwsWarningInfo()}
     * or {@link #getCmasWarningInfo()}.
     */
    private final int mServiceCategory;
    /** Message language, as a two-character string, e.g. "en". */
    @Nullable
    private final String mLanguage;
    /** The 8-bit data coding scheme defined in 3GPP TS 23.038 section 4. */
    private final int mDataCodingScheme;
    /** Message body, as a String. */
    @Nullable
    private final String mBody;
    /** Message priority (including emergency priority). */
    private final int mPriority;
    /** ETWS warning notification information (ETWS warnings only). */
    @Nullable
    private final SmsCbEtwsInfo mEtwsWarningInfo;
    /** CMAS warning notification information (CMAS warnings only). */
    @Nullable
    private final SmsCbCmasInfo mCmasWarningInfo;
    /**
     * Geo-Fencing Maximum Wait Time in second, a device shall allow to determine its position
     * meeting operator policy. If the device is unable to determine its position meeting operator
     * policy within the GeoFencing Maximum Wait Time, it shall present the alert to the user and
     * discontinue further positioning determination for the alert.
     */
    private final int mMaximumWaitTimeSec;
    /** UNIX timestamp of when the message was received. */
    private final long mReceivedTimeMillis;
    /** CMAS warning area coordinates. */
    private final List<CbGeoUtils.Geometry> mGeometries;
    private final int mSlotIndex;
    private final int mSubId;

    public SmsCbMessage(int messageFormat, int geographicalScope, int serialNumber,
                        @NonNull SmsCbLocation location, int serviceCategory, @Nullable String language,
                        @Nullable String body, int priority, @Nullable SmsCbEtwsInfo etwsWarningInfo,
                        @Nullable SmsCbCmasInfo cmasWarningInfo, int slotIndex, int subId) {
        this(messageFormat, geographicalScope, serialNumber, location, serviceCategory, language,
                0, body, priority, etwsWarningInfo, cmasWarningInfo, 0 /* maximumWaitingTime */,
                null /* geometries */, System.currentTimeMillis(), slotIndex, subId);
    }

    /**
     * Create a new {@link SmsCbMessage} with the specified data, including warning area
     * coordinates information.
     */
    public SmsCbMessage(int messageFormat, int geographicalScope, int serialNumber,
                        @NonNull SmsCbLocation location, int serviceCategory,
                        @Nullable String language, int dataCodingScheme, @Nullable String body,
                        int priority, @Nullable SmsCbEtwsInfo etwsWarningInfo,
                        @Nullable SmsCbCmasInfo cmasWarningInfo, int maximumWaitTimeSec,
                        @Nullable List<CbGeoUtils.Geometry> geometries, long receivedTimeMillis, int slotIndex,
                        int subId) {
        mMessageFormat = messageFormat;
        mGeographicalScope = geographicalScope;
        mSerialNumber = serialNumber;
        mLocation = location;
        mServiceCategory = serviceCategory;
        mLanguage = language;
        mDataCodingScheme = dataCodingScheme;
        mBody = body;
        mPriority = priority;
        mEtwsWarningInfo = etwsWarningInfo;
        mCmasWarningInfo = cmasWarningInfo;
        mReceivedTimeMillis = receivedTimeMillis;
        mGeometries = geometries;
        mMaximumWaitTimeSec = maximumWaitTimeSec;
        mSlotIndex = slotIndex;
        mSubId = subId;
    }
    /**
     * Create a new SmsCbMessage object from a Parcel.
     * @hide
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public SmsCbMessage(@NonNull Parcel in) {
        mMessageFormat = in.readInt();
        mGeographicalScope = in.readInt();
        mSerialNumber = in.readInt();
        mLocation = new SmsCbLocation(in);
        mServiceCategory = in.readInt();
        mLanguage = in.readString();
        mDataCodingScheme = in.readInt();
        mBody = in.readString();
        mPriority = in.readInt();
        int type = in.readInt();
        switch (type) {
            case 'E':
                // unparcel ETWS warning information
                mEtwsWarningInfo = new SmsCbEtwsInfo(in);
                mCmasWarningInfo = null;
                break;
            case 'C':
                // unparcel CMAS warning information
                mEtwsWarningInfo = null;
                mCmasWarningInfo = new SmsCbCmasInfo(in);
                break;
            default:
                mEtwsWarningInfo = null;
                mCmasWarningInfo = null;
        }
        mReceivedTimeMillis = in.readLong();
        String geoStr = in.readString();
        mGeometries = geoStr != null ? CbGeoUtils.parseGeometriesFromString(geoStr) : null;
        mMaximumWaitTimeSec = in.readInt();
        mSlotIndex = in.readInt();
        mSubId = in.readInt();
    }
    /**
     * Flatten this object into a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written (ignored).
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMessageFormat);
        dest.writeInt(mGeographicalScope);
        dest.writeInt(mSerialNumber);
        mLocation.writeToParcel(dest, flags);
        dest.writeInt(mServiceCategory);
        dest.writeString(mLanguage);
        dest.writeInt(mDataCodingScheme);
        dest.writeString(mBody);
        dest.writeInt(mPriority);
        if (mEtwsWarningInfo != null) {
            // parcel ETWS warning information
            dest.writeInt('E');
            mEtwsWarningInfo.writeToParcel(dest, flags);
        } else if (mCmasWarningInfo != null) {
            // parcel CMAS warning information
            dest.writeInt('C');
            mCmasWarningInfo.writeToParcel(dest, flags);
        } else {
            // no ETWS or CMAS warning information
            dest.writeInt('0');
        }
        dest.writeLong(mReceivedTimeMillis);
        dest.writeString(
                mGeometries != null ? CbGeoUtils.encodeGeometriesToString(mGeometries) : null);
        dest.writeInt(mMaximumWaitTimeSec);
        dest.writeInt(mSlotIndex);
        dest.writeInt(mSubId);
    }
    @NonNull
    public static final Creator<SmsCbMessage> CREATOR =
            new Creator<SmsCbMessage>() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public SmsCbMessage createFromParcel(Parcel in) {
                    return new SmsCbMessage(in);
                }
                @Override
                public SmsCbMessage[] newArray(int size) {
                    return new SmsCbMessage[size];
                }
            };
    /**
     * Return the geographical scope of this message (GSM/UMTS only).
     *
     * @return Geographical scope
     */
    public @GeographicalScope int getGeographicalScope() {
        return mGeographicalScope;
    }
    /**
     * Return the broadcast serial number of broadcast (message identifier for CDMA, or
     * geographical scope + message code + update number for GSM/UMTS). The serial number plus
     * the location code uniquely identify a cell broadcast for duplicate detection.
     *
     * @return the 16-bit CDMA message identifier or GSM/UMTS serial number
     */
    public int getSerialNumber() {
        return mSerialNumber;
    }

    /**
     * Return the 16-bit CDMA service category or GSM/UMTS message identifier. The interpretation
     * of the category is radio technology specific. For ETWS and CMAS warnings, the information
     * provided by the category is available via {@link #getEtwsWarningInfo()} or
     * {@link #getCmasWarningInfo()} in a radio technology independent format.
     *
     * @return the radio technology specific service category
     */
    public int getServiceCategory() {
        return mServiceCategory;
    }
    /**
     * Get the ISO-639-1 language code for this message, or null if unspecified
     *
     * @return Language code
     */
    @Nullable
    public String getLanguageCode() {
        return mLanguage;
    }
    /**
     * Get data coding scheme of the message
     *
     * @return The 8-bit data coding scheme defined in 3GPP TS 23.038 section 4.
     */
    public int getDataCodingScheme() {
        return mDataCodingScheme;
    }
    /**
     * Get the body of this message, or null if no body available
     *
     * @return Body, or null
     */
    @Nullable
    public String getMessageBody() {
        return mBody;
    }
    /**
     * Get the warning area coordinates information represented by polygons and circles.
     * @return a list of geometries, or an empty list if there is no coordinate information
     * associated with this message.
     */

    public List<CbGeoUtils.Geometry> getGeometries() {
        if (mGeometries == null) {
            return new ArrayList<>();
        }
        return mGeometries;
    }

    /**
     * Get the time when this message was received.
     * @return the time in millisecond
     */
    public long getReceivedTime() {
        return mReceivedTimeMillis;
    }
    /**
     * Get the slot index associated with this message.
     * @return the slot index associated with this message
     */

    public int getSubscriptionId() {
        return mSubId;
    }


    @Nullable
    public SmsCbEtwsInfo getEtwsWarningInfo() {
        return mEtwsWarningInfo;
    }
    /**
     * If this is a CMAS warning notification then this method will return an object containing
     * the CMAS message class, category, response type, severity, urgency and certainty.
     * The message class is always present. Severity, urgency and certainty are present for CDMA
     * warning notifications containing a type 1 elements record and for GSM and UMTS warnings
     * except for the Presidential-level alert category. Category and response type are only
     * available for CDMA notifications containing a type 1 elements record.
     *
     * @return an SmsCbCmasInfo object, or null if this is not a CMAS warning notification
     */
    @Nullable
    public SmsCbCmasInfo getCmasWarningInfo() {
        return mCmasWarningInfo;
    }
    /**
     * Return whether this message is an emergency (PWS) message type.
     * @return true if the message is an emergency notification; false otherwise
     */

    public boolean isEtwsMessage() {
        return mEtwsWarningInfo != null;
    }
    /**
     * Return whether this message is a CMAS warning alert.
     * @return true if the message is a CMAS warning notification; false otherwise
     */
    public boolean isCmasMessage() {
        return mCmasWarningInfo != null;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public String toString() {
        return "SmsCbMessage{geographicalScope=" + mGeographicalScope + ", serialNumber="
                + mSerialNumber + ", location=" + mLocation + ", serviceCategory="
                + mServiceCategory + ", language=" + mLanguage + ", body=" + mBody
                + ", priority=" + mPriority
                + (mEtwsWarningInfo != null ? (", " + mEtwsWarningInfo.toString()) : "")
                + (mCmasWarningInfo != null ? (", " + mCmasWarningInfo.toString()) : "")
                + ", maximumWaitingTime=" + mMaximumWaitTimeSec
                + ", received time=" + mReceivedTimeMillis
                + ", slotIndex = " + mSlotIndex
                + ", geo=" + (mGeometries != null
                ? CbGeoUtils.encodeGeometriesToString(mGeometries) : "null")
                + '}';
    }
    /**
     * Describe the kinds of special objects contained in the marshalled representation.
     * @return a bitmask indicating this Parcelable contains no special objects
     */
    @Override
    public int describeContents() {
        return 0;
    }


    /**
     * @return {@code True} if this message needs geo-fencing check.
     */
    public boolean needGeoFencingCheck() {
        return mMaximumWaitTimeSec > 0 && mGeometries != null && !mGeometries.isEmpty();
    }
}
