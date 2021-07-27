package com.github.gotify.messages.provider;

import android.app.Activity;
import com.github.gotify.Utils;
import com.github.gotify.api.ApiException;
import com.github.gotify.api.Callback;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.ApplicationApi;
import com.github.gotify.client.model.Application;
import java.util.Collections;
import java.util.List;

public class ApplicationHolder {
    private List<Application> state;
    private Runnable onUpdate;
    private Runnable onUpdateFailed;
    private Activity activity;
    private ApiClient client;

    public ApplicationHolder(Activity activity, ApiClient client) {
        this.activity = activity;
        this.client = client;
    }

    public void requestIfMissing() {
        if (state == null) {
            request();
        }
    }

    public void request() {
        client.createService(ApplicationApi.class)
                .getApps()
                .enqueue(Callback.callInUI(activity, this::onReceiveApps, this::onFailedApps));
    }

    private void onReceiveApps(List<Application> apps) {
        state = apps;
        if (onUpdate != null) onUpdate.run();
    }

    private void onFailedApps(ApiException e) {
        Utils.showSnackBar(activity, "Could not request applications, see logs.");
        if (onUpdateFailed != null) onUpdateFailed.run();
    }

    public List<Application> get() {
        return state == null ? Collections.emptyList() : state;
    }

    public void onUpdate(Runnable runnable) {
        this.onUpdate = runnable;
    }

    public void onUpdateFailed(Runnable runnable) {
        this.onUpdateFailed = runnable;
    }
}
