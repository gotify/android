package com.github.gotify.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.github.gotify.log.Log;

public class ReconnectListener extends BroadcastReceiver {

    private Runnable reconnected;

    ReconnectListener(Runnable reconnected) {
        this.reconnected = reconnected;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo network = cm.getActiveNetworkInfo();

        if (network != null && network.isConnected()) {
            Log.i("Network reconnected");
            reconnected.run();
        }
    }
}
