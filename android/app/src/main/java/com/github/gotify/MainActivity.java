package com.github.gotify;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import com.facebook.react.ReactActivity;

public class MainActivity extends ReactActivity {

    @Override
    protected void onStart() {
        super.onStart();
        if (!isPushServiceRunning()) {
            Log.i("MainActivity starting PushService");
            startService(new Intent(this, PushService.class));
        }
    }

    private boolean isPushServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PushService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the name of the main component registered from JavaScript.
     * This is used to schedule rendering of the component.
     */
    @Override
    protected String getMainComponentName() {
        return "gotify";
    }
}
