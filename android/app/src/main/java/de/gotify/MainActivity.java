package de.gotify;

import android.content.Intent;

import com.facebook.react.ReactActivity;

public class MainActivity extends ReactActivity {

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, PushService.class));
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
