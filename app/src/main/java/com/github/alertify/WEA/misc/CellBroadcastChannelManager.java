package com.github.alertify.WEA.misc;

import android.annotation.NonNull;
import android.content.Context;
import android.util.Log;

import com.github.alertify.WEA.CBUtils.SmsCbMessage;
import com.github.alertify.WEA.CellBroadcastAlertService;

import java.util.ArrayList;
import java.util.Arrays;

public class CellBroadcastChannelManager {

    private static final String TAG = "CBChannelManager";
    private final Context mContext;
    private final int mSubId;

    public static final int ROAMING_TYPE_NOT_ROAMING = 0;
    public static class CellBroadcastChannelRange {
        /** Defines the type of the alert. */
        private static final String KEY_TYPE = "type";
        /** Defines if the alert is emergency. */
        private static final String KEY_EMERGENCY = "emergency";
        /** Defines the network RAT for the alert. */
        private static final String KEY_RAT = "rat";
        /** Defines the scope of the alert. */
        private static final String KEY_SCOPE = "scope";
        /** Defines the vibration pattern of the alert. */
        private static final String KEY_VIBRATION = "vibration";
        /** Defines the duration of the alert. */
        private static final String KEY_ALERT_DURATION = "alert_duration";
        /** Defines if Do Not Disturb should be overridden for this alert */
        private static final String KEY_OVERRIDE_DND = "override_dnd";
        /** Defines whether writing alert message should exclude from SMS inbox. */
        private static final String KEY_EXCLUDE_FROM_SMS_INBOX = "exclude_from_sms_inbox";

        private static final String KEY_FILTER_LANGUAGE = "filter_language";


        public static final int SCOPE_UNKNOWN = 0;
        public static final int SCOPE_CARRIER = 1;
        public static final int SCOPE_DOMESTIC = 2;
        public static final int SCOPE_INTERNATIONAL = 3;

        public static final int LEVEL_UNKNOWN = 0;
        public static final int LEVEL_NOT_EMERGENCY = 1;
        public static final int LEVEL_EMERGENCY = 2;

        public int mStartId;
        public int mEndId;
        public CellBroadcastAlertService.AlertType mAlertType;
        public int mEmergencyLevel;
        public int mRanType;
        public int mScope;
        public int[] mVibrationPattern;
        public boolean mFilterLanguage;
        // by default no custom alert duration. play the alert tone with the tone's duration.
        public int mAlertDuration = -1;
        public boolean mOverrideDnd = false;
        // If enable_write_alerts_to_sms_inbox is true, write to sms inbox is enabled by default
        // for all channels except for channels which explicitly set to exclude from sms inbox.
        public boolean mWriteToSmsInbox = true;

        public CellBroadcastChannelRange(Context context, int subId, String channelRange) {

            mAlertType = CellBroadcastAlertService.AlertType.DEFAULT;
            mEmergencyLevel = LEVEL_UNKNOWN;
            mRanType = SmsCbMessage.MESSAGE_FORMAT_3GPP;
            mScope = SCOPE_UNKNOWN;
            mFilterLanguage = false;

            int colonIndex = channelRange.indexOf(':');
            if (colonIndex != -1) {
                // Parse the alert type and emergency flag
                String[] pairs = channelRange.substring(colonIndex + 1).trim().split(",");
                for (String pair : pairs) {
                    pair = pair.trim();
                    String[] tokens = pair.split("=");
                    if (tokens.length == 2) {
                        String key = tokens[0].trim();
                        String value = tokens[1].trim();
                        switch (key) {
                            case KEY_TYPE:
                                mAlertType = CellBroadcastAlertService.AlertType.valueOf(value.toUpperCase());
                                break;
                            case KEY_EMERGENCY:
                                if (value.equalsIgnoreCase("true")) {
                                    mEmergencyLevel = LEVEL_EMERGENCY;
                                } else if (value.equalsIgnoreCase("false")) {
                                    mEmergencyLevel = LEVEL_NOT_EMERGENCY;
                                }
                                break;
                            case KEY_RAT:
                                mRanType = value.equalsIgnoreCase("cdma")
                                        ? SmsCbMessage.MESSAGE_FORMAT_3GPP2 :
                                        SmsCbMessage.MESSAGE_FORMAT_3GPP;
                                break;
                            case KEY_SCOPE:
                                if (value.equalsIgnoreCase("carrier")) {
                                    mScope = SCOPE_CARRIER;
                                } else if (value.equalsIgnoreCase("domestic")) {
                                    mScope = SCOPE_DOMESTIC;
                                } else if (value.equalsIgnoreCase("international")) {
                                    mScope = SCOPE_INTERNATIONAL;
                                }
                                break;
                            case KEY_VIBRATION:
                                String[] vibration = value.split("\\|");
                                if (vibration.length > 0) {
                                    mVibrationPattern = new int[vibration.length];
                                    for (int i = 0; i < vibration.length; i++) {
                                        mVibrationPattern[i] = Integer.parseInt(vibration[i]);
                                    }
                                }
                                break;
                            case KEY_FILTER_LANGUAGE:
                                if (value.equalsIgnoreCase("true")) {
                                    mFilterLanguage = true;
                                }
                                break;
                            case KEY_ALERT_DURATION:
                                mAlertDuration = Integer.parseInt(value);
                                break;
                            case KEY_OVERRIDE_DND:
                                if (value.equalsIgnoreCase("true")) {
                                    mOverrideDnd = true;
                                }
                                break;
                            case KEY_EXCLUDE_FROM_SMS_INBOX:
                                if (value.equalsIgnoreCase("true")) {
                                    mWriteToSmsInbox = false;
                                }
                                break;
                        }
                    }
                }
                channelRange = channelRange.substring(0, colonIndex).trim();
            }

            // Parse the channel range
            int dashIndex = channelRange.indexOf('-');
            if (dashIndex != -1) {
                // range that has start id and end id
                mStartId = Integer.decode(channelRange.substring(0, dashIndex).trim());
                mEndId = Integer.decode(channelRange.substring(dashIndex + 1).trim());
            } else {
                // Not a range, only a single id
                mStartId = mEndId = Integer.decode(channelRange);
            }
        }

        @Override
        public String toString() {
            return "Range:[channels=" + mStartId + "-" + mEndId + ",emergency level="
                    + mEmergencyLevel + ",type=" + mAlertType + ",scope=" + mScope + ",vibration="
                    + Arrays.toString(mVibrationPattern) + ",alertDuration=" + mAlertDuration
                    + ",filter_language=" + mFilterLanguage + ",override_dnd=" + mOverrideDnd + "]";
        }
    }

    /**
     * Constructor
     *
     * @param context Context
     * @param subId Subscription index
     */
    public CellBroadcastChannelManager(Context context, int subId) {
        mContext = context;
        mSubId = subId;
    }

    public boolean checkCellBroadcastChannelRange(int channel, int key) {
        ArrayList<CellBroadcastChannelRange> ranges = getCellBroadcastChannelRanges(key);

        for (CellBroadcastChannelRange range : ranges) {
            if (channel >= range.mStartId && channel <= range.mEndId) {
                return checkScope(range.mScope);
            }
        }

        return false;
    }

    public @NonNull
    ArrayList<CellBroadcastChannelRange> getCellBroadcastChannelRanges(int key) {
        ArrayList<CellBroadcastChannelRange> result = new ArrayList<>();
        String[] ranges =
                CellBroadcastSettings.getResources(mContext, mSubId).getStringArray(key);

        for (String range : ranges) {
            try {
                result.add(new CellBroadcastChannelRange(mContext, mSubId, range));
            } catch (Exception e) {
                loge("Failed to parse \"" + range + "\". e=" + e);
            }
        }

        return result;
    }

    public boolean checkScope(int rangeScope) {
        return true;
    }
    /**
     * Check if the cell broadcast message is an emergency message or not
     *
     * @param message Cell broadcast message
     * @return True if the message is an emergency message, otherwise false.
     */
    public boolean isEmergencyMessage(SmsCbMessage message) {
        if (message == null) {
            return false;
        } else {
            return true;
        }
    }
    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
