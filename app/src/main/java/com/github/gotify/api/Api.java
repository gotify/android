package com.github.gotify.api;

import android.app.Activity;
import com.github.gotify.client.ApiCallback;
import com.github.gotify.client.ApiException;
import com.github.gotify.log.Log;
import java.util.List;
import java.util.Map;

public class Api {
    public static <T> CallExecutor<T> withLogging(ApiCall<T> call) {
        return new CallExecutor<T>(call);
    }

    private static <T> Callback<T> loggingCallback() {
        return Callback.call(
                (data) -> {},
                (exception) ->
                        Log.e("Error while api call: " + exception.getResponseBody(), exception));
    }

    public interface ApiCall<T> {
        void call(ApiCallback<T> callback) throws ApiException;
    }

    public static class CallExecutor<T> {
        private ApiCall<T> call;

        private CallExecutor(ApiCall<T> call) {
            this.call = call;
        }

        public void handle(Callback<T> callback) {
            Callback<T> merged = Callback.merge(callback, loggingCallback());
            try {
                call.call(fromCallback(merged));
            } catch (ApiException e) {
                merged.onError(e);
            }
        }

        public void handleInUIThread(
                Activity activity,
                Callback.SuccessCallback<T> onSuccess,
                Callback.ErrorCallback onError) {
            handle(Callback.runInUIThread(activity, Callback.call(onSuccess, onError)));
        }

        public void handle(Callback.SuccessCallback<T> onSuccess, Callback.ErrorCallback onError) {
            handle(Callback.call(onSuccess, onError));
        }
    }

    private static <T> ApiCallback<T> fromCallback(Callback<T> callback) {
        return new ApiCallback<T>() {
            @Override
            public void onFailure(
                    ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                callback.onError(e);
            }

            @Override
            public void onSuccess(
                    T result, int statusCode, Map<String, List<String>> responseHeaders) {
                callback.onSuccess(result);
            }

            @Override
            public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {}

            @Override
            public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {}
        };
    }
}
