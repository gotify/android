package com.github.gotify.settings;

import android.content.Context;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import com.github.gotify.R;

public final class ThemeHelper {
    private ThemeHelper() {}

    public static void setTheme(Context context, String newTheme) {
        AppCompatDelegate.setDefaultNightMode(ofKey(context, newTheme));
    }

    private static int ofKey(Context context, String newTheme) {
        if (context.getString(R.string.theme_dark).equals(newTheme)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        if (context.getString(R.string.theme_light).equals(newTheme)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
