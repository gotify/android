package com.github.gotify.api

import android.app.Activity
import com.github.gotify.log.Log
import retrofit2.Call
import retrofit2.Response

internal class Callback<T> private constructor(
    private val onSuccess: SuccessCallback<T>,
    private val onError: ErrorCallback
) {
    fun interface SuccessCallback<T> {
        fun onSuccess(data: T?)
    }

    fun interface ErrorCallback {
        fun onError(t: ApiException)
    }

    private class RetrofitCallback<T>(private val callback: Callback<T>) : retrofit2.Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                callback.onSuccess.onSuccess(response.body())
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
                {
                    context.runOnUiThread {
                        onSuccess.onSuccess(it)
                    }
                },
                {
                    context.runOnUiThread {
                        onError.onError(it)
                    }
                })
        }

        fun <T> call(): retrofit2.Callback<T> {
            return call({},{})
        }

        fun <T> call(onSuccess: SuccessCallback<T>, onError: ErrorCallback): retrofit2.Callback<T> {
            return RetrofitCallback(merge(of(onSuccess, onError), errorCallback()))
        }

        private fun <T> of(
            onSuccess: SuccessCallback<T>,
            onError: ErrorCallback
        ): Callback<T> {
            return Callback(onSuccess, onError)
        }

        private fun <T> errorCallback(): Callback<T> {
            return Callback({}, { Log.e("Error while api call", it) })
        }

        private fun <T> merge(left: Callback<T>, right: Callback<T>): Callback<T> {
            return Callback(
                {
                    left.onSuccess.onSuccess(it)
                    right.onSuccess.onSuccess(it)
                },
                {
                    left.onError.onError(it)
                    right.onError.onError(it)
                }
            )
        }
    }
}
