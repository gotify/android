package com.github.gotify.api;

import android.app.Activity;
import com.github.gotify.log.Log;
import retrofit2.Call;
import retrofit2.Response;

public class Callback<T> {
    private final SuccessCallback<T> onSuccess;
    private final ErrorCallback onError;

    private Callback(SuccessCallback<T> onSuccess, ErrorCallback onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    public static <T> retrofit2.Callback<T> callInUI(
            Activity context, SuccessCallback<T> onSuccess, ErrorCallback onError) {
        return call(
                (data) -> context.runOnUiThread(() -> onSuccess.onSuccess(data)),
                (e) -> context.runOnUiThread(() -> onError.onError(e)));
    }

    public static <T> retrofit2.Callback<T> call() {
        return call((e) -> {}, (e) -> {});
    }

    public static <T> retrofit2.Callback<T> call(
            SuccessCallback<T> onSuccess, ErrorCallback onError) {
        return new RetrofitCallback<>(merge(of(onSuccess, onError), errorCallback()));
    }

    private static <T> Callback<T> of(SuccessCallback<T> onSuccess, ErrorCallback onError) {
        return new Callback<>(onSuccess, onError);
    }

    private static <T> Callback<T> errorCallback() {
        return new Callback<>((ignored) -> {}, (error) -> Log.e("Error while api call", error));
    }

    private static <T> Callback<T> merge(Callback<T> left, Callback<T> right) {
        return new Callback<>(
                (data) -> {
                    left.onSuccess.onSuccess(data);
                    right.onSuccess.onSuccess(data);
                },
                (error) -> {
                    left.onError.onError(error);
                    right.onError.onError(error);
                });
    }

    public interface SuccessCallback<T> {
        void onSuccess(T data);
    }

    public interface ErrorCallback {
        void onError(ApiException t);
    }

    private static final class RetrofitCallback<T> implements retrofit2.Callback<T> {

        private Callback<T> callback;

        private RetrofitCallback(Callback<T> callback) {
            this.callback = callback;
        }

        @Override
        public void onResponse(Call<T> call, Response<T> response) {
            if (response.isSuccessful()) {
                callback.onSuccess.onSuccess(response.body());
            } else {
                callback.onError.onError(new ApiException(response));
            }
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            callback.onError.onError(new ApiException(t));
        }
    }
}
