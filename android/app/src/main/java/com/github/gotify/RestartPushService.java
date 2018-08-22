package com.github.gotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RestartPushService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Received restart broadcast; restarting now.");
        context.startService(new Intent(context, PushService.class));
    }
}
