package com.github.gotify.api

import retrofit2.Call
import java.io.IOException

internal object Api {
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
