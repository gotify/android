package com.github.alertify.WEA.CBUtils;

class SmsConstants {
    /** User data text encoding code unit size */
    public static final int ENCODING_UNKNOWN = 0;
    public static final int ENCODING_7BIT = 1;
    public static final int ENCODING_8BIT = 2;
    public static final int ENCODING_16BIT = 3;

    /** The maximum number of payload septets per message */
    public static final int MAX_USER_DATA_SEPTETS = 160;

    /**
     * The maximum number of payload septets per message if a user data header
     * is present.  This assumes the header only contains the
     * CONCATENATED_8_BIT_REFERENCE element.
     */
    public static final int MAX_USER_DATA_SEPTETS_WITH_HEADER = 153;

    /**
     * This value is not defined in global standard. Only in Korea, this is used.
     */
    public static final int ENCODING_KSC5601 = 4;

    /** The maximum number of payload bytes per message */
    public static final int MAX_USER_DATA_BYTES = 140;

    /**
     * The maximum number of payload bytes per message if a user data header
     * is present.  This assumes the header only contains the
     * CONCATENATED_8_BIT_REFERENCE element.
     */
    public static final int MAX_USER_DATA_BYTES_WITH_HEADER = 134;

    /**
     * SMS Class enumeration.
     * See TS 23.038.
     */
    public enum MessageClass{

        UNKNOWN,
        CLASS_0,

        CLASS_1,

        CLASS_2,

        CLASS_3;
    }


    public static final String FORMAT_UNKNOWN = "unknown";


    public static final String FORMAT_3GPP = "3gpp";

    public static final String FORMAT_3GPP2 = "3gpp2";
}