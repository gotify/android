package com.github.gotify.api

import java.io.IOException
import retrofit2.Call

internal object Api {
    @Throws(ApiException::class)
    fun execute(call: Call<Void>) {
        try {
            val response = call.execute()

            if (!response.isSuccessful) {
                throw ApiException(response)
            }
        } catch (e: IOException) {
            throw ApiException(e)
        }
    }

    @Throws(ApiException::class)
    fun <T> execute(call: Call<T>): T {
        try {
            val response = call.execute()

            if (response.isSuccessful) {
                return response.body() ?: throw ApiException("null response", response)
            } else {
                throw ApiException(response)
            }
        } catch (e: IOException) {
            throw ApiException(e)
        }
    }
}
