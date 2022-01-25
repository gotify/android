package com.github.gotify.messages;

import android.app.Activity;
import androidx.lifecycle.ViewModel;
import com.github.gotify.Settings;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.MessageApi;
import com.github.gotify.messages.provider.ApplicationHolder;
import com.github.gotify.messages.provider.MessageFacade;
import com.github.gotify.messages.provider.MessageState;
import com.github.gotify.picasso.PicassoHandler;
import com.squareup.picasso.Target;
import java.util.ArrayList;
import java.util.List;

public class MessagesModel extends ViewModel {
    private final Settings settings;
    private final PicassoHandler picassoHandler;
    private final ApiClient client;
    private final ApplicationHolder appsHolder;
    private final MessageFacade messages;

    // we need to keep the target references otherwise they get gc'ed before they can be called.
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Target> targetReferences = new ArrayList<>();

    private long appId = MessageState.ALL_MESSAGES;

    public MessagesModel(Activity parentView) {
        settings = new Settings(parentView);
        picassoHandler = new PicassoHandler(parentView, settings);
        client =
                ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token());
        appsHolder = new ApplicationHolder(parentView, client);
        messages = new MessageFacade(client.createService(MessageApi.class), appsHolder);
    }

    public Settings getSettings() {
        return settings;
    }

    public PicassoHandler getPicassoHandler() {
        return picassoHandler;
    }

    public ApiClient getClient() {
        return client;
    }

    public ApplicationHolder getAppsHolder() {
        return appsHolder;
    }

    public MessageFacade getMessages() {
        return messages;
    }

    public List<Target> getTargetReferences() {
        return targetReferences;
    }

    public long getAppId() {
        return appId;
    }

    public void setAppId(long appId) {
        this.appId = appId;
    }
}
