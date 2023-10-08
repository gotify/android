package com.github.gotify.api

import android.app.Activity
import com.github.gotify.api.Callback.ErrorCallback
import com.github.gotify.api.Callback.SuccessCallback
import org.tinylog.kotlin.Logger
import retrofit2.Call
import retrofit2.Response

internal class Callback<T> private constructor(
    private val onSuccess: SuccessCallback<T>,
    private val onError: ErrorCallback
) {
    fun interface SuccessCallback<T> {
        fun onSuccess(response: Response<T>)
    }

    fun interface SuccessBody<T> : SuccessCallback<T> {
        override fun onSuccess(response: Response<T>) {
            onResultSuccess(response.body() ?: throw ApiException("null response", response))
        }

        fun onResultSuccess(data: T)
    }

    fun interface ErrorCallback {
        fun onError(t: ApiException)
    }

    private class RetrofitCallback<T>(private val callback: Callback<T>) : retrofit2.Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                callback.onSuccess.onSuccess(response)
            } else {
                callback.onError.onError(ApiException(response))
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            callback.onError.onError(ApiException(t))
        }
    }

    companion object {
        fun <T> callInUI(
            context: Activity,
            onSuccess: SuccessCallback<T>,
            onError: ErrorCallback
        ): retrofit2.Callback<T> {
            return call(
                onSuccess = { response -> context.runOnUiThread { onSuccess.onSuccess(response) } },
                onError = { exception -> context.runOnUiThread { onError.onError(exception) } }
            )
        }

        fun <T> call(
            onSuccess: SuccessCallback<T> = SuccessCallback {},
            onError: ErrorCallback = ErrorCallback {}
        ): retrofit2.Callback<T> {
            return RetrofitCallback(merge(of(onSuccess, onError), errorCallback()))
        }

        private fun <T> of(onSuccess: SuccessCallback<T>, onError: ErrorCallback): Callback<T> {
            return Callback(onSuccess, onError)
        }

        private fun <T> errorCallback(): Callback<T> {
            return Callback(
                onSuccess = {},
                onError = { exception -> Logger.error(exception, "Error while api call") }
            )
        }

        private fun <T> merge(left: Callback<T>, right: Callback<T>): Callback<T> {
            return Callback(
                onSuccess = { data ->
                    left.onSuccess.onSuccess(data)
                    right.onSuccess.onSuccess(data)
                },
                onError = { exception ->
                    left.onError.onError(exception)
                    right.onError.onError(exception)
                }
            )
        }
    }
}
