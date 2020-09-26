package com.github.alertify;

import com.github.alertify.api.Api;
import com.github.alertify.api.ApiException;
import com.github.alertify.api.Callback;
import com.github.alertify.client.api.MessageApi;
import com.github.alertify.client.model.Message;
import com.github.alertify.client.model.PagedMessages;
import com.github.alertify.log.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.alertify.api.Callback.call;

public class MissedMessageUtil {
    static final long NO_MESSAGES = 0;

    private final MessageApi api;

    public MissedMessageUtil(MessageApi api) {
        this.api = api;
    }

    public void lastReceivedMessage(Callback.SuccessCallback<Long> successCallback) {
        api.getMessages(1, 0L)
                .enqueue(
                        call(
                                (messages) -> {
                                    if (messages.getMessages().size() == 1) {
                                        successCallback.onSuccess(
                                                messages.getMessages().get(0).getId());
                                    } else {
                                        successCallback.onSuccess(NO_MESSAGES);
                                    }
                                },
                                (e) -> {}));
    }

    public List<Message> missingMessages(long till) {
        List<Message> result = new ArrayList<>();
        try {

            Long since = null;
            while (true) {
                PagedMessages pagedMessages = Api.execute(api.getMessages(10, since));
                List<Message> messages = pagedMessages.getMessages();
                List<Message> filtered = filter(messages, till);
                result.addAll(filtered);
                if (messages.size() != filtered.size()
                        || messages.size() == 0
                        || pagedMessages.getPaging().getNext() == null) {
                    break;
                }
                since = pagedMessages.getPaging().getSince();
            }
        } catch (ApiException e) {
            Log.e("cannot retrieve missing messages", e);
        }
        Collections.reverse(result);
        return result;
    }

    private List<Message> filter(List<Message> messages, long till) {
        List<Message> result = new ArrayList<>();

        for (Message message : messages) {
            if (message.getId() > till) {
                result.add(message);
            } else {
                break;
            }
        }

        return result;
    }
}
