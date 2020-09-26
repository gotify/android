package com.github.alertify.WEA.misc;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Settings activity for the cell broadcast receiver.
 */
public class CellBroadcastSettings extends Activity {
    // Resource cache
    private static final Map<Integer, Resources> sResourcesCache = new HashMap<>();
    // Test override for disabling the subId specific resources
    private static boolean sUseResourcesForSubId = true;
    public static final int DEFAULT_SUBSCRIPTION_ID = 2147483647;// 0x7fffffff
    public static final int INVALID_SUBSCRIPTION_ID = -1;

    public static boolean isValidSubscriptionId(int subscriptionId) {
        return subscriptionId > INVALID_SUBSCRIPTION_ID;
    }


    public static Resources getResources( Context context, int subId) {
        if (subId == DEFAULT_SUBSCRIPTION_ID
                || !isValidSubscriptionId(subId) || !sUseResourcesForSubId) {
            return context.getResources();
        }
        if (sResourcesCache.containsKey(subId)) {
            return sResourcesCache.get(subId);
        }
        Resources res = getResourcesForSubId(context, subId);
        sResourcesCache.put(subId, res);
        return res;
    }

    public static Resources getResourcesForSubId(Context context, int subId) {
        return getResourcesForSubId(context, subId, false);
    }

    /**
     * Returns the resources associated with Subscription.
     * @param context Context object
     * @param subId Subscription Id of Subscription who's resources are required
     * @param useRootLocale if root locale should be used. Localized locale is used if false.
     * @return Resources associated with Subscription.
     */
    public static Resources getResourcesForSubId(Context context, int subId,  boolean useRootLocale) {
        Configuration config = context.getResources().getConfiguration();
        Configuration newConfig = new Configuration();
        newConfig.setTo(config);
        newConfig.mcc = 310;
        newConfig.mnc = 110;

        if (useRootLocale) {
            newConfig.setLocale(Locale.ROOT);
        }

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        DisplayMetrics newMetrics = new DisplayMetrics();
        newMetrics.setTo(metrics);
        return new Resources(context.getResources().getAssets(), newMetrics, newConfig);
    }



}
