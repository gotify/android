package com.github.gotify.api;

import android.app.Activity;
import com.github.gotify.client.ApiException;

public class Callback<T> {
    private final SuccessCallback<T> onSuccess;
    private final ErrorCallback onError;

    private Callback(SuccessCallback<T> onSuccess, ErrorCallback onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    void onSuccess(T data) {
        this.onSuccess.onSuccess(data);
    }

    void onError(ApiException exception) {
        this.onError.onError(exception);
    }

    public static <T> Callback<T> call(SuccessCallback<T> onSuccess, ErrorCallback onError) {
        return new Callback<>(onSuccess, onError);
    }

    public static <T> Callback<T> merge(Callback<T> left, Callback<T> right) {
        return new Callback<>(
                (data) -> {
                    left.onSuccess(data);
                    right.onSuccess(data);
                },
                (error) -> {
                    left.onError(error);
                    right.onError(error);
                });
    }

    public static <T> Callback<T> runInUIThread(Activity context, Callback<T> callback) {
        return call(
                (data) -> {
                    context.runOnUiThread(() -> callback.onSuccess(data));
                },
                (e) -> {
                    context.runOnUiThread(() -> callback.onError(e));
                });
    }

    public interface SuccessCallback<T> {
        void onSuccess(T data);
    }

    public interface ErrorCallback {
        void onError(ApiException e);
    }
}
